package jsonschema_to_mermaid

import jsonschema_to_mermaid.jsonschema.Property
import jsonschema_to_mermaid.schema_files.SchemaFileInfo

/**
 * Preferences for customizing Mermaid diagram generation.
 */
data class Preferences(
    val showRequiredWithPlus: Boolean = true,
    val arraysAsRelation: Boolean = true,
)

/**
 * Generates Mermaid class diagrams from JSON Schema files.
 */
object MermaidGenerator {

    /**
     * Generate a Mermaid class diagram from a collection of schema files.
     * @param schemaFiles The schema files to process.
     * @param preferences Preferences for diagram generation.
     * @return Mermaid class diagram as a string.
     */
    fun generate(schemaFiles: Collection<SchemaFileInfo>, noClassDiagramHeader: Boolean = false, preferences: Preferences = Preferences()): String {
        loadedSchemas = schemaFiles.toList()
        val classProperties = linkedMapOf<String, MutableList<String>>()
        val relations = mutableListOf<String>()
        processDefinitions(schemaFiles, classProperties, relations, preferences)
        processTopLevelSchemas(schemaFiles, classProperties, relations, preferences)
        return buildOutput(classProperties, relations, noClassDiagramHeader)
    }

    /**
     * Process schema definitions and populate class properties and relations.
     */
    private fun processDefinitions(
        schemaFiles: Collection<SchemaFileInfo>,
        classProperties: MutableMap<String, MutableList<String>>,
        relations: MutableList<String>,
        preferences: Preferences
    ) {
        schemaFiles.forEach { schemaFile ->
            schemaFile.schema.definitions?.forEach { (definitionName, definitionSchema) ->
                val className = sanitizeName(definitionName)
                ensureClassEntry(classProperties, className)
                definitionSchema.properties?.forEach { (propertyName, property) ->
                    val isRequired = definitionSchema.required?.contains(propertyName) == true
                    mapPropertyToClass(property, propertyName, classProperties[className]!!, preferences, isRequired)
                    addRelationForDefinitionProperty(className, propertyName, property, relations, preferences)
                }
            }
        }
    }

    /**
     * Add a relation for a property in a definition, if applicable.
     */
    private fun addRelationForDefinitionProperty(
        className: String,
        propertyName: String,
        property: Property,
        relations: MutableList<String>,
        preferences: Preferences
    ) {
        if (property.`$ref` != null) {
            val target = refToClassName(property.`$ref`)
            relations.add(formatRelation(className, target, null, null, propertyName, "o--"))
        }
        if (property.type == "array" && preferences.arraysAsRelation) {
            val items = property.items
            if (items?.`$ref` != null) {
                val target = refToClassName(items.`$ref`)
                relations.add(formatRelation(className, target, "1", "*", propertyName, "-->"))
            }
        }
    }

    /**
     * Process top-level schemas and populate class properties and relations.
     */
    private fun processTopLevelSchemas(
        schemaFiles: Collection<SchemaFileInfo>,
        classProperties: MutableMap<String, MutableList<String>>,
        relations: MutableList<String>,
        preferences: Preferences
    ) {
        schemaFiles.forEach { schemaFile ->
            val className = getClassName(schemaFile)
            ensureClassEntry(classProperties, className)

            handleInheritance(schemaFile, className, relations)

            schemaFile.schema.properties?.forEach { (propertyName, property) ->
                // If property is inherited, skip entirely (do not show in child)
                if (isInheritedProperty(schemaFile, propertyName)) {
                    return@forEach
                }

                val isRequired = schemaFile.schema.required?.contains(propertyName) == true

                if (handleCompositionKeywords(classProperties, className, propertyName, property, relations, preferences)) return@forEach
                if (handleOneOrAnyOf(classProperties, className, propertyName, property, relations, preferences)) return@forEach

                // Maps (additionalProperties)
                if (property.additionalProperties != null) {
                    classProperties[className]!!.add(formatField(propertyName, property, preferences, isRequired))
                    return@forEach
                }

                // Maps (patternProperties)
                if (property.patternProperties != null) {
                    classProperties[className]!!.add(formatField(propertyName, property, preferences, isRequired))
                    return@forEach
                }

                handleTopLevelProperty(schemaFile, className, propertyName, property, classProperties, relations, preferences, isRequired)
            }
        }
    }

    /**
     * Handle inheritance (extends) for a schema file.
     */
    private fun handleInheritance(schemaFile: SchemaFileInfo, className: String, relations: MutableList<String>) {
        val extends = schemaFile.schema.extends
        if (extends != null) {
            val parentClassName = when (extends) {
                is jsonschema_to_mermaid.jsonschema.Extends.Ref -> {
                    // Try to resolve the parent schema file by filename or title
                    val ref = extends.ref
                    val parentSchemaFile = findSchemaByRef(ref)
                    if (parentSchemaFile != null) getClassName(parentSchemaFile) else refToClassName(ref)
                }
                is jsonschema_to_mermaid.jsonschema.Extends.Object -> {
                    val ref = extends.ref
                    val parentSchemaFile = findSchemaByRef(ref)
                    if (parentSchemaFile != null) getClassName(parentSchemaFile) else refToClassName(ref)
                }
            }
            relations.add("$parentClassName <|-- $className")
        }
    }

    /**
     * Find a schema file by $ref string, matching filename or title.
     */
    private fun findSchemaByRef(ref: String?): SchemaFileInfo? {
        if (ref == null) return null
        // Try to match by filename (strip extension) or by title
        return loadedSchemas.firstOrNull { schemaFile ->
            val baseName = schemaFile.filename?.substringBefore('.')
            baseName != null && ref.contains(baseName) ||
            (schemaFile.schema.title != null && ref.contains(schemaFile.schema.title))
        }
    }

    // Store loaded schemas for reference resolution
    private var loadedSchemas: List<SchemaFileInfo> = emptyList()

    /**
     * Check if a property is inherited.
     */
    private fun isInheritedProperty(schemaFile: SchemaFileInfo, propertyName: String): Boolean {
        val inheritedSet = schemaFile.schema.inheritedPropertyNames?.toSet() ?: emptySet()
        return inheritedSet.contains(propertyName)
    }

    /**
     * Handle a top-level property, dispatching to the correct handler.
     */
    private fun handleTopLevelProperty(
        schemaFile: SchemaFileInfo,
        className: String,
        propertyName: String,
        property: Property,
        classProperties: MutableMap<String, MutableList<String>>,
        relations: MutableList<String>,
        preferences: Preferences,
        isRequired: Boolean
    ) {
        when {
            property.`$ref` != null -> relations.add(formatRelation(className, refToClassName(property.`$ref`), if (isRequired) "1" else "0..1", "1", propertyName, "-->"))
            property.type == "array" && preferences.arraysAsRelation -> handleTopLevelArray(schemaFile, className, propertyName, property, classProperties, relations, preferences, isRequired)
            property.type == "object" -> handleTopLevelObject(className, propertyName, property, classProperties, relations, preferences, isRequired)
            else -> classProperties[className]!!.add(formatField(propertyName, property, preferences, isRequired))
        }
    }

    private fun handleCompositionKeywords(
        classProperties: MutableMap<String, MutableList<String>>,
        className: String,
        propertyName: String,
        property: Property,
        relations: MutableList<String>,
        preferences: Preferences
    ): Boolean {
        if (!property.allOf.isNullOrEmpty()) {
            val refs = property.allOf.filter { it.`$ref` != null }
            val inlines = property.allOf.filter { it.`$ref` == null && it.properties != null }
            var handled = false
            if (refs.isNotEmpty()) {
                refs.forEach { r ->
                    relations.add(formatRelation(className, refToClassName(r.`$ref`), "1", "1", propertyName, "-->"))
                }
                handled = true
            }
            if (inlines.isNotEmpty()) {
                // Merge inline properties into the parent class
                inlines.forEach { inline ->
                    inline.properties?.forEach { (inlinePropName, inlineProp) ->
                        classProperties[className]?.add(
                            formatField(inlinePropName, inlineProp, preferences, inline.required?.contains(inlinePropName) == true)
                        )
                    }
                }
                handled = true
            }
            return handled
        }
        return false
    }

    private fun handleOneOrAnyOf(
        classProperties: MutableMap<String, MutableList<String>>,
        className: String,
        propertyName: String,
        property: Property,
        relations: MutableList<String>,
        preferences: Preferences
    ): Boolean {
        fun processMembers(members: List<Property>?, label: String): Boolean {
            if (members.isNullOrEmpty()) return false
            members.forEach { member ->
                when {
                    member.`$ref` != null -> relations.add(formatRelation(className, refToClassName(member.`$ref`), "1", "1", "$propertyName ($label)", "-->"))
                    member.type == "object" -> {
                        val target = sanitizeName(propertyName) + "-option"
                        ensureClassEntry(classProperties, target)
                        val subProperties = member.properties ?: emptyMap()
                        subProperties.forEach { (innerPropertyName, innerProperty) ->
                            val subRequired = member.required?.contains(innerPropertyName) == true
                            mapPropertyToClass(innerProperty, innerPropertyName, classProperties[target]!!, preferences, subRequired)
                        }
                        relations.add(formatRelation(className, target, "1", "1", "$propertyName ($label)", "-->"))
                    }
                }
            }
            return true
        }
        return processMembers(property.oneOf, "oneOf") || processMembers(property.anyOf, "anyOf")
    }

    private fun handleTopLevelArray(
        schemaFile: SchemaFileInfo,
        className: String,
        propertyName: String,
        property: Property,
        classProperties: MutableMap<String, MutableList<String>>,
        relations: MutableList<String>,
        preferences: Preferences,
        isRequired: Boolean
    ) {
        val items = property.items
        if (items?.`$ref` != null) {
            relations.add(formatRelation(className, refToClassName(items.`$ref`), "1", "*", propertyName, "-->"))
        } else if (items?.type == "object") {
            val base = if (propertyName.endsWith("s")) propertyName.dropLast(1) else propertyName
            val parent = className.trim().ifEmpty { getClassName(schemaFile) }
            val target = parent + sanitizeName(base).replaceFirstChar { it.uppercaseChar() }
            ensureClassEntry(classProperties, target)
            val itemProperties = items.properties ?: emptyMap()
            itemProperties.forEach { (innerPropertyName, innerProperty) ->
                val subRequired = items.required?.contains(innerPropertyName) == true
                mapPropertyToClass(innerProperty, innerPropertyName, classProperties[target]!!, preferences, subRequired)
            }
            relations.add(formatRelation(className, target, "1", "*", propertyName, "-->"))
        } else {
            classProperties[className]!!.add(formatArrayField(propertyName, items, isRequired))
        }
    }

    private fun handleTopLevelObject(
        className: String,
        propertyName: String,
        property: Property,
        classProperties: MutableMap<String, MutableList<String>>,
        relations: MutableList<String>,
        preferences: Preferences,
        isRequired: Boolean
    ) {
        val target = sanitizeName(propertyName)
        ensureClassEntry(classProperties, target)
        val subProperties = property.properties ?: emptyMap()
        subProperties.forEach { (innerPropertyName, innerProperty) ->
            val subRequired = property.required?.contains(innerPropertyName) == true
            mapPropertyToClass(innerProperty, innerPropertyName, classProperties[target]!!, preferences, subRequired)
        }
        val multiplicity = if (isRequired) "1" else "0..1"
        relations.add(formatRelation(className, target, multiplicity, "1", propertyName, "-->"))
    }

    // ---- Mapping & formatting ----
    private fun mapPropertyToClass(property: Property, propertyName: String, targetProperties: MutableList<String>, preferences: Preferences, isRequired: Boolean) {
        when {
            property.`$ref` != null -> targetProperties.add(formatInlineField(propertyName, refToClassName(property.`$ref`), isRequired))
            property.type == "array" -> {
                val items = property.items
                if (!preferences.arraysAsRelation) targetProperties.add(formatArrayField(propertyName, items, isRequired))
                else if (items?.type != "object" && items?.`$ref` == null) targetProperties.add(formatArrayField(propertyName, items, isRequired))
            }
            property.type == "object" -> {
                val target = sanitizeName(propertyName)
                targetProperties.add(formatInlineField(propertyName, target, isRequired))
            }
            else -> targetProperties.add(formatField(propertyName, property, preferences, isRequired))
        }
    }

    private fun formatInlineField(propertyName: String, refType: String, isRequired: Boolean): String {
        return if (isRequired) {
            "+$refType $propertyName"
        } else {
            "$refType $propertyName [0..1]"
        }
    }

    private fun formatField(propertyName: String, property: Property?, preferences: Preferences, isRequired: Boolean = false): String {
        if (property?.additionalProperties != null) {
            val additional = property.additionalProperties
            var mapped = "Object"
            if (additional is Map<*, *>) {
                val t = additional["type"] as? String
                mapped = primitiveTypeName(t)
            }
            return if (isRequired) {
                "+Map<String,$mapped> $propertyName"
            } else {
                "Map<String,$mapped> $propertyName [0..1]"
            }
        }
        if (property?.patternProperties != null) {
            // Get the first pattern property to determine the value type
            val firstPattern = property.patternProperties.entries.firstOrNull()
            var mapped = "Object"
            if (firstPattern != null) {
                val patternProp = firstPattern.value
                mapped = primitiveTypeName(patternProp.type ?: patternProp.format)
            }
            return if (isRequired) {
                "+Map<String,$mapped> $propertyName"
            } else {
                "Map<String,$mapped> $propertyName [0..1]"
            }
        }
        val t = property?.type ?: property?.format
        val kotlinType = primitiveTypeName(t)
        return when {
            property == null -> if (isRequired) "+$kotlinType $propertyName" else "$kotlinType $propertyName [0..1]"
            property.type == "array" && property.items != null && !preferences.arraysAsRelation -> {
                val itemType = property.items.type ?: property.items.format
                val mapped = primitiveTypeName(itemType)
                if (isRequired) "+$mapped[] $propertyName" else "$mapped[] $propertyName [0..1]"
            }
            property.`$ref` != null -> {
                val refName = refToClassName(property.`$ref`)
                if (isRequired) "+$refName $propertyName" else "$refName $propertyName [0..1]"
            }
            else -> if (isRequired) "+$kotlinType $propertyName" else "$kotlinType $propertyName [0..1]"
        }
    }

    private fun formatArrayField(propertyName: String, items: Property?, isRequired: Boolean): String {
        val itemType = items?.type ?: items?.format
        val mapped = primitiveTypeName(itemType)
        return if (isRequired) "+$mapped[] $propertyName" else "$mapped[] $propertyName [0..1]"
    }

    private fun formatRelation(fromClassName: String, toClassName: String, fromMultiplicity: String? = null, toMultiplicity: String? = null, label: String, arrow: String = "-->"): String {
        val fromPart = if (fromMultiplicity != null) "\"$fromMultiplicity\" " else ""
        val toPart = if (toMultiplicity != null) " \"$toMultiplicity\"" else ""
        return "$fromClassName $fromPart$arrow$toPart $toClassName : $label"
    }

    private fun ensureClassEntry(classProperties: MutableMap<String, MutableList<String>>, className: String) {
        if (!classProperties.containsKey(className)) classProperties[className] = mutableListOf()
    }

    private fun buildOutput(classProperties: Map<String, List<String>>, relations: List<String>, noClassDiagramHeader: Boolean = false): String {
        val sb = StringBuilder()
        if (!noClassDiagramHeader) sb.append("classDiagram\n")
        classProperties.forEach { (className, properties) ->
            sb.append("  class $className {\n")
            properties.forEach { sb.append("    $it\n") }
            sb.append("  }\n")
        }
        if (relations.isNotEmpty()) sb.append('\n')
        relations.forEach { sb.append("  $it\n") }
        return sb.toString()
    }

    // ---- Name & type helpers ----
    private fun primitiveTypeName(typeOrFormat: String?): String = when (typeOrFormat) {
        "integer" -> "Integer"
        "number" -> "Number"
        "boolean" -> "Boolean"
        "string" -> "String"
        null -> "Object"
        else -> typeOrFormat.replaceFirstChar { it.uppercaseChar().toString() }
    }

    /**
     * Sanitize a name for use as a class or property identifier in Mermaid diagrams.
     * Converts to PascalCase for class names.
     */
    private fun sanitizeName(name: String?): String = name?.split(Regex("[^A-Za-z0-9]+")).orEmpty()
        .filter { it.isNotBlank() }
        .joinToString("") { it.replaceFirstChar { c -> c.uppercaseChar() } }

    /**
     * Get the class name for a schema file, using its title (if present) or filename, in PascalCase.
     */
    private fun getClassName(schemaFile: SchemaFileInfo): String =
        schemaFile.schema.title?.let { sanitizeName(it) }
            ?: schemaFile.filename?.substringBefore('.')?.let { sanitizeName(it) }
            ?: "UnknownSchema"

    /**
     * Convert a JSON Schema $ref string to a class name.
     */
    private fun refToClassName(ref: String?): String {
        if (ref == null) return "UnknownRef"
        // Extract the last part after '/' or '#' and sanitize
        val parts = ref.split('/', '#').filter { it.isNotBlank() }
        return sanitizeName(parts.lastOrNull() ?: ref)
    }
}
