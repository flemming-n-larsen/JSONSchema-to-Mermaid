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

        processDefinitions(schemas, classProps, prefs)
        processTopLevelSchemas(schemas, classProps, relations, prefs)

        return buildOutput(classProps, relations)
    }

    // --- Processing helpers ---
    private fun processDefinitions(schemas: Collection<SchemaFileInfo>, classProps: MutableMap<String, MutableList<String>>, prefs: Preferences) {
        schemas.forEach { file ->
            file.schema.definitions?.forEach { (defName, defSchema) ->
                val className = defName.replaceFirstChar { it.uppercaseChar().toString() }
                if (!classProps.containsKey(className)) {
                    classProps[className] = mutableListOf()
                    defSchema.properties?.forEach { (pname, pprop) ->
                        mapPropertyToClass(pprop, pname, classProps[className]!!, className, defSchema.required ?: listOf(), prefs)
                    }
                }
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
            if (!classProps.containsKey(className)) classProps[className] = mutableListOf()

            schemaFile.schema.properties?.forEach { (pname, prop) ->
                when {
                    prop.`$ref` != null -> {
                        val target = refToClassName(prop.`$ref`)
                        relations.add(formatRelation(className, target, "1", pname))
                    }
                    prop.type == "array" && prefs.arraysAsRelation -> {
                        val items = prop.items
                        if (items?.`$ref` != null) {
                            val target = refToClassName(items.`$ref`)
                            relations.add(formatRelation(className, target, "*", pname))
                        } else if (items?.type == "object") {
                            val target = className + pname.replaceFirstChar { it.uppercaseChar().toString() } + "Item"
                            if (!classProps.containsKey(target)) classProps[target] = mutableListOf()
                            val itemProps = items.properties ?: emptyMap()
                            itemProps.forEach { (ipname, iprop) ->
                                mapPropertyToClass(iprop, ipname, classProps[target]!!, target, itemProps.keys.toList(), prefs)
                            }
                            relations.add(formatRelation(className, target, "*", pname))
                        } else {
                            // primitive array -> render as field type[]
                            classProps[className]!!.add(formatArrayField(pname, items, schemaFile.schema.required ?: listOf(), prefs))
                        }
                    }
                    prop.type == "object" -> {
                        val target = className + pname.replaceFirstChar { it.uppercaseChar().toString() }
                        if (!classProps.containsKey(target)) classProps[target] = mutableListOf()
                        val subProps = prop.properties ?: emptyMap()
                        subProps.forEach { (ipname, iprop) ->
                            mapPropertyToClass(iprop, ipname, classProps[target]!!, target, subProps.keys.toList(), prefs)
                        }
                        val multiplicity = if (schemaFile.schema.required?.contains(pname) == true) "\"1\"" else "\"0..1\""
                        relations.add("$className $multiplicity --> \"1\" $target : $pname")
                    }
                    else -> {
                        classProps[className]!!.add(formatField(pname, prop, schemaFile.schema.required ?: listOf(), prefs))
                    }
                }
            }
        }
    }

    private fun formatRelation(fromClass: String, toClass: String, toMult: String, label: String): String {
        // from-side multiplicity is always 1 in current usage
        return "$fromClass 1 --> $toMult $toClass : $label"
    }

    // map a single property into class property string
    private fun mapPropertyToClass(prop: Property, name: String, targetProps: MutableList<String>, parentClassName: String, requiredKeys: List<String>, prefs: Preferences) {
        when {
            prop.`$ref` != null -> {
                val refName = refToClassName(prop.`$ref`)
                targetProps.add(formatInlineField(name, refName, requiredKeys, prefs))
            }
            prop.type == "array" -> {
                val items = prop.items
                if (!prefs.arraysAsRelation) {
                    targetProps.add(formatArrayField(name, items, requiredKeys, prefs))
                } else {
                    // arrays as relation will generally be represented by a relation; if primitive, still show inline
                    if (items?.type != "object" && items?.`$ref` == null) targetProps.add(formatArrayField(name, items, requiredKeys, prefs))
                }
            }
            prop.type == "object" -> {
                val target = parentClassName + name.replaceFirstChar { it.uppercaseChar().toString() }
                targetProps.add(formatInlineField(name, target, requiredKeys, prefs))
            }
            else -> {
                targetProps.add(formatField(name, prop, requiredKeys, prefs))
            }
        }
    }

    private fun formatInlineField(name: String, refType: String, requiredKeys: List<String>, prefs: Preferences): String {
        val prefix = if (prefs.showRequiredWithPlus && requiredKeys.contains(name)) "+" else ""
        return "$prefix$refType $name"
    }

    private fun formatField(name: String, prop: Property?, requiredKeys: List<String>, prefs: Preferences): String {
        val t = prop?.format ?: prop?.type
        val kotlinType = primitiveTypeName(t)
        val prefix = if (prefs.showRequiredWithPlus && requiredKeys.contains(name)) "+" else ""
        return when {
            prop == null -> "$prefix$kotlinType $name"
            prop.type == "array" && prop.items != null && !prefs.arraysAsRelation -> {
                val itemType = prop.items.format ?: prop.items.type
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

    private fun formatArrayField(name: String, items: Property?, requiredKeys: List<String>, prefs: Preferences): String {
        val itemType = items?.format ?: items?.type
        val mapped = primitiveTypeName(itemType)
        val prefix = if (prefs.showRequiredWithPlus && requiredKeys.contains(name)) "+" else ""
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
