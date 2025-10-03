package jsonschema_to_mermaid

import jsonschema_to_mermaid.schema_files.SchemaFileInfo
import jsonschema_to_mermaid.jsonschema.Property
import jsonschema_to_mermaid.jsonschema.Schema

// Preferences for visual/layout choices
data class Preferences(
    val showRequiredWithPlus: Boolean = true,
    val arraysAsRelation: Boolean = true // when true, arrays create relations with "*" multiplicity
)

object MermaidGenerator {

    fun generate(schemas: Collection<SchemaFileInfo>, prefs: Preferences = Preferences()): String {
        val classProps = linkedMapOf<String, MutableList<String>>()
        val relations = mutableListOf<String>()

        processDefinitions(schemas, classProps, relations, prefs)
        processTopLevelSchemas(schemas, classProps, relations, prefs)

        return buildOutput(classProps, relations)
    }

    // --- Processing helpers ---
    private fun processDefinitions(
        schemas: Collection<SchemaFileInfo>,
        classProps: MutableMap<String, MutableList<String>>,
        relations: MutableList<String>,
        prefs: Preferences
    ) {
        schemas.forEach { file ->
            file.schema.definitions?.forEach { (defName, defSchema) ->
                val className = sanitizeName(defName)
                ensureClassEntry(classProps, className)

                defSchema.properties?.forEach { (pname, pprop) ->
                    mapPropertyToClass(pprop, pname, classProps[className]!!, prefs)
                    addRelationForDefinitionProperty(className, pname, pprop, relations, prefs)
                }
            }
        }
    }

    private fun addRelationForDefinitionProperty(
        className: String,
        pname: String,
        pprop: Property,
        relations: MutableList<String>,
        prefs: Preferences
    ) {
        // if property is a $ref -> also emit an aggregation relation (e.g., Product o-- Money : price)
        if (pprop.`$ref` != null) {
            val target = refToClassName(pprop.`$ref`)
            relations.add(formatRelation(className, target, null, null, pname, "o--"))
        }

        // if property is an array of $ref -> emit relation with multiplicity
        if (pprop.type == "array" && prefs.arraysAsRelation) {
            val items = pprop.items
            if (items?.`$ref` != null) {
                val target = refToClassName(items.`$ref`)
                relations.add(formatRelation(className, target, "1", "*", pname, "-->"))
            }
        }
    }

    private fun processTopLevelSchemas(
        schemas: Collection<SchemaFileInfo>,
        classProps: MutableMap<String, MutableList<String>>,
        relations: MutableList<String>,
        prefs: Preferences
    ) {
        schemas.forEach { schemaFile ->
            val className = getClassName(schemaFile)
            ensureClassEntry(classProps, className)

            // Add inheritance arrow if extends is present
            val extends = schemaFile.schema.extends
            if (extends != null) {
                val parentClassName = when (extends) {
                    is jsonschema_to_mermaid.jsonschema.Extends.Ref -> refToClassName(extends.ref)
                    is jsonschema_to_mermaid.jsonschema.Extends.Object -> refToClassName(extends.ref)
                    else -> null
                }
                if (parentClassName != null) relations.add("$className <|-- $parentClassName")
            }

            schemaFile.schema.properties?.forEach { (pname, prop) ->
                // prioritize composition-based handling
                if (handleCompositionKeywords(className, pname, prop, relations)) return@forEach

                // maps at top-level -> render as Map and continue
                if (prop.additionalProperties != null) {
                    classProps[className]!!.add(formatField(pname, prop, prefs))
                    return@forEach
                }

                if (handleOneOrAnyOf(classProps, className, pname, prop, relations, prefs)) return@forEach

                handleStandardTopLevelProperty(schemaFile, className, pname, prop, classProps, relations, prefs)
            }
        }
    }

    private fun handleCompositionKeywords(className: String, pname: String, prop: Property, relations: MutableList<String>): Boolean {
        if (!prop.allOf.isNullOrEmpty()) {
            val refs = prop.allOf.filter { it.`$ref` != null }
            if (refs.isNotEmpty()) {
                refs.forEach { r ->
                    // we filtered refs to those where `$ref` is non-null, so use it directly
                    relations.add(formatRelation(className, refToClassName(r.`$ref`), "1", "1", pname, "-->"))
                }
                return true
            }
        }
        return false
    }

    private fun handleOneOrAnyOf(
        classProps: MutableMap<String, MutableList<String>>,
        className: String,
        pname: String,
        prop: Property,
        relations: MutableList<String>,
        prefs: Preferences
    ): Boolean {
        if (!prop.oneOf.isNullOrEmpty()) {
            prop.oneOf.forEach { member ->
                when {
                    member.`$ref` != null -> relations.add(formatRelation(className, refToClassName(member.`$ref`), "1", "1", "$pname (oneOf)", "-->"))
                    member.type == "object" -> {
                        val target = sanitizeName(pname) + "-option"
                        ensureClassEntry(classProps, target)
                        val subProps = member.properties ?: emptyMap()
                        subProps.forEach { (ipname, iprop) -> mapPropertyToClass(iprop, ipname, classProps[target]!!, prefs) }
                        relations.add(formatRelation(className, target, "1", "1", "$pname (oneOf)", "-->"))
                    }
                }
            }
            return true
        }

        if (!prop.anyOf.isNullOrEmpty()) {
            prop.anyOf.forEach { member ->
                when {
                    member.`$ref` != null -> relations.add(formatRelation(className, refToClassName(member.`$ref`), "1", "1", "$pname (anyOf)", "-->"))
                    member.type == "object" -> {
                        val target = sanitizeName(pname) + "-option"
                        ensureClassEntry(classProps, target)
                        val subProps = member.properties ?: emptyMap()
                        subProps.forEach { (ipname, iprop) -> mapPropertyToClass(iprop, ipname, classProps[target]!!, prefs) }
                        relations.add(formatRelation(className, target, "1", "1", "$pname (anyOf)", "-->"))
                    }
                }
            }
            return true
        }

        return false
    }

    private fun handleStandardTopLevelProperty(
        schemaFile: SchemaFileInfo,
        className: String,
        pname: String,
        prop: Property,
        classProps: MutableMap<String, MutableList<String>>,
        relations: MutableList<String>,
        prefs: Preferences
    ) {
        when {
            prop.`$ref` != null -> relations.add(formatRelation(className, refToClassName(prop.`$ref`), "1", "1", pname, "-->"))
            prop.type == "array" && prefs.arraysAsRelation -> handleTopLevelArray(schemaFile, className, pname, prop, classProps, relations, prefs)
            prop.type == "object" -> handleTopLevelObject(schemaFile, className, pname, prop, classProps, relations, prefs)
            else -> classProps[className]!!.add(formatField(pname, prop, prefs))
        }
    }

    private fun handleTopLevelArray(
        schemaFile: SchemaFileInfo,
        className: String,
        pname: String,
        prop: Property,
        classProps: MutableMap<String, MutableList<String>>,
        relations: MutableList<String>,
        prefs: Preferences
    ) {
        val items = prop.items
        if (items?.`$ref` != null) {
            relations.add(formatRelation(className, refToClassName(items.`$ref`), "1", "*", pname, "-->"))
        } else if (items?.type == "object") {
            val base = if (pname.endsWith("s")) pname.dropLast(1) else pname
            val parent = className.trim().ifEmpty { getClassName(schemaFile) }
            val target = parent + sanitizeName(base).replaceFirstChar { it.uppercaseChar() }
            ensureClassEntry(classProps, target)
            val itemProps = items.properties ?: emptyMap()
            itemProps.forEach { (ipname, iprop) -> mapPropertyToClass(iprop, ipname, classProps[target]!!, prefs) }
            relations.add(formatRelation(className, target, "1", "*", pname, "-->"))
        } else {
            // primitive array -> render as field type[]
            classProps[className]!!.add(formatArrayField(pname, items, prefs))
        }
    }

    private fun handleTopLevelObject(
        schemaFile: SchemaFileInfo,
        className: String,
        pname: String,
        prop: Property,
        classProps: MutableMap<String, MutableList<String>>,
        relations: MutableList<String>,
        prefs: Preferences
    ) {
        val target = sanitizeName(pname)
        ensureClassEntry(classProps, target)
        val subProps = prop.properties ?: emptyMap()
        subProps.forEach { (ipname, iprop) -> mapPropertyToClass(iprop, ipname, classProps[target]!!, prefs) }
        val multiplicity = if (schemaFile.schema.required?.contains(pname) == true) "1" else "0..1"
        relations.add(formatRelation(className, target, multiplicity, "1", pname, "-->"))
    }

    private fun ensureClassEntry(classProps: MutableMap<String, MutableList<String>>, className: String) {
        if (!classProps.containsKey(className)) classProps[className] = mutableListOf()
    }

    // format relation with optional multiplicities and arrow type
    private fun formatRelation(fromClass: String, toClass: String, fromMult: String? = null, toMult: String? = null, label: String, arrow: String = "-->"): String {
        val fromPart = if (fromMult != null) "\"$fromMult\" " else ""
        val toPart = if (toMult != null) " \"$toMult\"" else ""
        // if arrow is aggregation (o--), we don't print multiplicities by default unless provided
        // Use sanitized/unquoted class names (refToClassName/getClassName already sanitize names)
        return "$fromClass $fromPart$arrow$toPart $toClass : $label"
    }

    // map a single property into class property string
    private fun mapPropertyToClass(prop: Property, name: String, targetProps: MutableList<String>, prefs: Preferences) {
        when {
            prop.`$ref` != null -> targetProps.add(formatInlineField(name, refToClassName(prop.`$ref`), prefs))
            prop.type == "array" -> {
                val items = prop.items
                if (!prefs.arraysAsRelation) targetProps.add(formatArrayField(name, items, prefs))
                else if (items?.type != "object" && items?.`$ref` == null) targetProps.add(formatArrayField(name, items, prefs))
            }
            prop.type == "object" -> {
                val target = sanitizeName(name)
                targetProps.add(formatInlineField(name, target, prefs))
            }
            else -> targetProps.add(formatField(name, prop, prefs))
        }
    }

    private fun formatInlineField(name: String, refType: String, prefs: Preferences): String {
        val prefix = if (prefs.showRequiredWithPlus) "+" else ""
        return "$prefix$refType $name"
    }

    private fun formatField(name: String, prop: Property?, prefs: Preferences): String {
        // handle additionalProperties as a Map<K,V>
        if (prop?.additionalProperties != null) {
            val add = prop.additionalProperties
            var mapped = "Object"
            if (add is Map<*, *>) {
                val t = add["type"] as? String
                mapped = primitiveTypeName(t)
            }
            val prefix = if (prefs.showRequiredWithPlus) "+" else ""
            return prefix + "Map<String,$mapped> " + name
        }

        val t = prop?.type ?: prop?.format
        val kotlinType = primitiveTypeName(t)
        val prefix = if (prefs.showRequiredWithPlus) "+" else ""
        return when {
            prop == null -> "$prefix$kotlinType $name"
            prop.type == "array" && prop.items != null && !prefs.arraysAsRelation -> {
                val itemType = prop.items.type ?: prop.items.format
                val mapped = primitiveTypeName(itemType)
                "$prefix$mapped[] $name"
            }
            prop.`$ref` != null -> {
                val refName = refToClassName(prop.`$ref`)
                "$prefix$refName $name"
            }
            else -> "$prefix$kotlinType $name"
        }
    }

    private fun formatArrayField(name: String, items: Property?, prefs: Preferences): String {
        val itemType = items?.type ?: items?.format
        val mapped = primitiveTypeName(itemType)
        val prefix = if (prefs.showRequiredWithPlus) "+" else ""
        return "$prefix$mapped[] $name"
    }

    private fun primitiveTypeName(typeOrFormat: String?): String {
        return when (typeOrFormat) {
            "integer" -> "Integer"
            "number" -> "Number"
            "boolean" -> "Boolean"
            "string" -> "String"
            null -> "Object"
            else -> typeOrFormat.replaceFirstChar { it.uppercaseChar().toString() }
        }
    }

    private fun refToClassName(ref: String?): String {
        if (ref == null) return "unknown"
        var rn = ref
        // If there's a fragment (#...), prefer the fragment content (internal ref like #/definitions/Foo)
        val hashIndex = rn.indexOf('#')
        if (hashIndex >= 0) {
            val frag = rn.substring(hashIndex + 1)
            rn = if (frag.isNotBlank()) frag else rn.substring(0, hashIndex)
        }
        // keep last path segment if path-like
        val last = rn.lastIndexOf('/')
        if (last >= 0) rn = rn.substring(last + 1)
        return sanitizeName(rn)
    }

    // sanitize arbitrary string into a valid class identifier (no spaces/dots etc.)
    private fun sanitizeName(name: String?): String {
        if (name == null) return "Unknown"
        var rn = name.trim()
        if (rn.isEmpty()) return "Unknown"
        // if it's a path-like name, take last segment
        val lastSlash = rn.lastIndexOf('/')
        if (lastSlash >= 0) rn = rn.substring(lastSlash + 1)
        // drop common file extension after first '.' to avoid things like 'file.schema.yaml'
        val dotIndex = rn.indexOf('.')
        if (dotIndex >= 0) rn = rn.substring(0, dotIndex)
        // If the name is a single alphanumeric token and already contains uppercase letters,
        // assume it's already CamelCase/PascalCase and preserve its capitalization (just strip non-alphanumerics).
        if (rn.matches(Regex("^[A-Za-z0-9]+$")) && rn.any { it.isUpperCase() }) {
            val cleaned = rn.replace(Regex("[^A-Za-z0-9]"), "")
            // if the token already contains camelCase/PascalCase boundaries, split on those boundaries
            val boundaryRegex = Regex("(?<=[a-z])(?=[A-Z])|(?<=[A-Z])(?=[A-Z][a-z])")
            if (boundaryRegex.containsMatchIn(cleaned)) {
                val parts = cleaned.split(boundaryRegex)
                val joined = parts.filter { it.isNotBlank() }
                    .joinToString(separator = "") { p -> p.lowercase().replaceFirstChar { it.uppercaseChar() } }
                return joined
            }
            return cleaned.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
        // replace non-alphanumeric characters with a space to split into words
        rn = rn.replace(Regex("[^A-Za-z0-9]+"), " ")
        // split into parts, capitalize each and join without separator -> PascalCase
        val parts = rn.split(Regex("\\s+"))
        val joined = parts.filter { it.isNotBlank() }
            .joinToString(separator = "") { part ->
                val lower = part.lowercase()
                lower.replaceFirstChar { it.uppercaseChar() }
            }
        // Fallback
        if (joined.isBlank()) return "Unknown"
        return joined.replace(Regex("[^A-Za-z0-9]"), "")
    }

    // Prefer titles: robustly parse CamelCase, snake-case, hyphenated, or space-separated titles into PascalCase.
    private fun sanitizeTitle(title: String?): String {
        if (title == null) return "Unknown"
        val t = title.trim()
        if (t.isEmpty()) return "Unknown"
        // Try to extract word tokens from CamelCase or other forms
        val wordRegex = Regex("[A-Z]?[a-z]+|[A-Z]+(?![a-z])|\\d+")
        val matches = wordRegex.findAll(t).map { it.value }.toList()
        val parts = if (matches.isNotEmpty()) matches else t.replace(Regex("[^A-Za-z0-9]+"), " ").split(Regex("\\s+"))
        val joined = parts.filter { it.isNotBlank() }
            .joinToString(separator = "") { p ->
                val lower = p.lowercase()
                lower.replaceFirstChar { it.uppercaseChar() }
            }
        return if (joined.isBlank()) "Unknown" else joined.replace(Regex("[^A-Za-z0-9]"), "")
    }

    // --- Output builder ---
    private fun buildOutput(classProps: Map<String, List<String>>, relations: List<String>): String {
        val sb = StringBuilder()
        sb.append("classDiagram\n")
        classProps.forEach { (className, props) ->
            sb.append("  class $className {\n")
            props.forEach { sb.append("    $it\n") }
            sb.append("  }\n")
        }
        if (relations.isNotEmpty()) sb.append('\n')
        relations.forEach { sb.append("  $it\n") }
        return sb.toString()
    }

    private fun getClassName(schemaData: SchemaFileInfo): String =
        // Prefer schema title when present, sanitized via sanitizeTitle so we preserve CamelCase like ProductCatalog
        (if (!schemaData.schema.title.isNullOrBlank()) sanitizeTitle(schemaData.schema.title) else sanitizeName(
            schemaData.schema.title
                ?: getClassNameFromFilePath(schemaData.filename)
                ?: getClassNameFromId(schemaData.schema)
        )).trim()

    private fun getClassNameFromFilePath(filepath: String?): String? {
        var classname = filepath
        classname?.indexOf(".")?.let { index ->
            if (index > 0) {
                classname = classname.substring(0, index)
            }
        }
        return classname
    }

    private fun getClassNameFromId(schema: Schema): String {
        var className = schema.`$id`?.trim() ?: throw IllegalStateException("jsonschema is missing title and \$id fields")
        val lastIndex = className.lastIndexOf("/")
        if (lastIndex >= 0) {
            className = className.substring(lastIndex + 1)
        }
        return className
    }
}
