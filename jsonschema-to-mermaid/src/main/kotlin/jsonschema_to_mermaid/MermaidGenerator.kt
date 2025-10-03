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
                val className = defName.replaceFirstChar { it.uppercaseChar().toString() }
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
                }
                relations.add("$className <|-- $parentClassName")
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
                    relations.add(formatRelation(className, refToClassName(r.`$ref`!!), "1", "1", pname, "-->"))
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
                    member.`$ref` != null -> relations.add(formatRelation(className, refToClassName(member.`$ref`!!), "1", "1", "$pname (oneOf)", "-->"))
                    member.type == "object" -> {
                        val target = pname.replaceFirstChar { it.uppercaseChar().toString() } + "Option"
                        ensureClassEntry(classProps, target)
                        val subProps = member.properties ?: emptyMap()
                        subProps.forEach { (ipname, iprop) ->
                            mapPropertyToClass(iprop, ipname, classProps[target]!!, prefs)
                        }
                        relations.add(formatRelation(className, target, "1", "1", "$pname (oneOf)", "-->"))
                    }
                }
            }
            return true
        }

        if (!prop.anyOf.isNullOrEmpty()) {
            prop.anyOf.forEach { member ->
                when {
                    member.`$ref` != null -> relations.add(formatRelation(className, refToClassName(member.`$ref`!!), "1", "1", "$pname (anyOf)", "-->"))
                    member.type == "object" -> {
                        val target = pname.replaceFirstChar { it.uppercaseChar().toString() } + "Option"
                        ensureClassEntry(classProps, target)
                        val subProps = member.properties ?: emptyMap()
                        subProps.forEach { (ipname, iprop) ->
                            mapPropertyToClass(iprop, ipname, classProps[target]!!, prefs)
                        }
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
            prop.`$ref` != null -> {
                val target = refToClassName(prop.`$ref`)
                relations.add(formatRelation(className, target, "1", "1", pname, "-->"))
            }
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
            val target = refToClassName(items.`$ref`)
            relations.add(formatRelation(className, target, "1", "*", pname, "-->"))
        } else if (items?.type == "object") {
            val base = if (pname.endsWith("s")) pname.dropLast(1) else pname
            val parent = className.trim().ifEmpty { getClassName(schemaFile) }
            val target = parent + base.replaceFirstChar { it.uppercaseChar().toString() }
            ensureClassEntry(classProps, target)
            val itemProps = items.properties ?: emptyMap()
            itemProps.forEach { (ipname, iprop) ->
                mapPropertyToClass(iprop, ipname, classProps[target]!!, prefs)
            }
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
        val target = pname.replaceFirstChar { it.uppercaseChar().toString() }
        ensureClassEntry(classProps, target)
        val subProps = prop.properties ?: emptyMap()
        subProps.forEach { (ipname, iprop) ->
            mapPropertyToClass(iprop, ipname, classProps[target]!!, prefs)
        }
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
        return "$fromClass $fromPart$arrow$toPart $toClass : $label"
    }

    // map a single property into class property string
    private fun mapPropertyToClass(prop: Property, name: String, targetProps: MutableList<String>, prefs: Preferences) {
        when {
            prop.`$ref` != null -> {
                val refName = refToClassName(prop.`$ref`)
                targetProps.add(formatInlineField(name, refName, prefs))
            }
            prop.type == "array" -> {
                val items = prop.items
                if (!prefs.arraysAsRelation) {
                    targetProps.add(formatArrayField(name, items, prefs))
                } else {
                    // arrays as relation will generally be represented by a relation; if primitive, still show inline
                    if (items?.type != "object" && items?.`$ref` == null) targetProps.add(formatArrayField(name, items, prefs))
                }
            }
            prop.type == "object" -> {
                val target = name.replaceFirstChar { it.uppercaseChar().toString() }
                targetProps.add(formatInlineField(name, target, prefs))
            }
            else -> {
                targetProps.add(formatField(name, prop, prefs))
            }
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

    private fun refToClassName(ref: String): String {
        var rn = ref
        val last = rn.lastIndexOf('/')
        if (last >= 0) rn = rn.substring(last + 1)
        return rn.replaceFirstChar { it.uppercaseChar().toString() }
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
        (schemaData.schema.title
            ?: getClassNameFromFilePath(schemaData.filename)
            ?: getClassNameFromId(schemaData.schema)).trim()

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
        var className =
            schema.`$id`?.trim() ?: throw IllegalStateException("jsonschema is missing title and \$id fields")
        val lastIndex = className.lastIndexOf("/")
        if (lastIndex >= 0) {
            className = className.substring(lastIndex + 1)
        }
        return className
    }
}
