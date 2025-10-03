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
        val classes = linkedMapOf<String, MutableList<String>>()
        val relations = mutableListOf<String>()

        // process definitions first so refs can reference them
        schemas.forEach { schemaFile ->
            schemaFile.schema.definitions?.forEach { (defName, defSchema) ->
                val className = defName.capitalize()
                if (!classes.containsKey(className)) {
                    classes[className] = mutableListOf()
                    defSchema.properties?.forEach { (pname, pprop) ->
                        // map properties for definition
                        mapPropertyToClass(pprop, pname, classes[className]!!, className, defSchema.required ?: listOf(), prefs, schemas)
                    }
                }
            }
        }

        // process top-level schemas
        schemas.forEach { schemaFile ->
            val className = getClassName(schemaFile)
            if (!classes.containsKey(className)) {
                classes[className] = mutableListOf()
            }
            schemaFile.schema.properties?.forEach { (pname, prop) ->
                // handle nested objects, arrays and refs by creating classes and relations
                when {
                    prop.`$ref` != null -> {
                        val target = refToClassName(prop.`$ref`!!)
                        relations.add("$className \"1\" --> \"1\" $target : $pname")
                    }
                    prop.type == "array" && prefs.arraysAsRelation -> {
                        // if items is ref
                        val items = prop.items
                        if (items?.`$ref` != null) {
                            val target = refToClassName(items.`$ref`!!)
                            relations.add("$className \"1\" --> \"*\" $target : $pname")
                        } else if (items?.type == "object") {
                            val target = (className + pname.capitalize() + "Item")
                            // create item class
                            if (!classes.containsKey(target)) classes[target] = mutableListOf()
                            items.properties?.forEach { (ipname, iprop) ->
                                mapPropertyToClass(iprop, ipname, classes[target]!!, target, items?.properties?.keys?.toList() ?: listOf(), prefs, schemas)
                            }
                            relations.add("$className \"1\" --> \"*\" $target : $pname")
                        } else {
                            // primitive array -> render as field type[]
                            classes[className]!!.add(formatArrayField(pname, items, schemaFile.schema.required ?: listOf(), prefs))
                        }
                    }
                    prop.type == "object" -> {
                        // extract nested object into its own class
                        val target = className + pname.capitalize()
                        if (!classes.containsKey(target)) classes[target] = mutableListOf()
                        prop.properties?.forEach { (ipname, iprop) ->
                            mapPropertyToClass(iprop, ipname, classes[target]!!, target, prop.properties?.keys?.toList() ?: listOf(), prefs, schemas)
                        }
                        val multiplicity = if (schemaFile.schema.required?.contains(pname) == true) "\"1\"" else "\"0..1\""
                        relations.add("$className $multiplicity --> \"1\" $target : $pname")
                    }
                    else -> {
                        // simple property
                        classes[className]!!.add(formatField(pname, prop, schemaFile.schema.required ?: listOf(), prefs))
                    }
                }
            }
        }

        // Build output
        val sb = StringBuilder()
        sb.append("classDiagram\n")
        classes.forEach { (className, props) ->
            sb.append("  class $className {")
            if (props.isNotEmpty()) {
                sb.append('\n')
                props.forEach { sb.append("    $it\n") }
                sb.append("  }")
            }
            sb.append('\n')
        }
        if (relations.isNotEmpty()) sb.append('\n')
        relations.forEach { sb.append("  $it\n") }

        return sb.toString()
    }

    // map a single property into class property string
    private fun mapPropertyToClass(prop: Property, name: String, targetProps: MutableList<String>, parentClassName: String, requiredKeys: List<String>, prefs: Preferences, schemas: Collection<SchemaFileInfo>) {
        when {
            prop.`$ref` != null -> {
                // reference field - create relation instead of inline field
                val refName = refToClassName(prop.`$ref`!!)
                // we won't add inline field for refs when mapping into separate class; caller may handle relations
                targetProps.add(formatInlineField(name, refName, requiredKeys, prefs))
            }
            prop.type == "array" -> {
                val items = prop.items
                if (!prefs.arraysAsRelation) {
                    // render as type[] inline
                    targetProps.add(formatArrayField(name, items, requiredKeys, prefs))
                } else {
                    // arrays as relation will be handled by parent generator; here just produce inline if primitive
                    if (items?.type != "object" && items?.`$ref` == null) targetProps.add(formatArrayField(name, items, requiredKeys, prefs))
                }
            }
            prop.type == "object" -> {
                // create nested class
                val target = parentClassName + name.capitalize()
                // ensure created elsewhere by caller; here we just add a field referencing it
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
        val kotlinType = when (t) {
            "integer" -> "Integer"
            "number" -> "Number"
            "boolean" -> "Boolean"
            "string" -> "String"
            else -> t?.capitalize() ?: "Object"
        }
        val prefix = if (prefs.showRequiredWithPlus && requiredKeys.contains(name)) "+" else ""
        return when {
            prop == null -> "$prefix$kotlinType $name"
            prop.type == "array" && prop.items != null && !prefs.arraysAsRelation -> {
                val itemType = prop.items.format ?: prop.items.type
                val mapped = when (itemType) {
                    "integer" -> "Integer"
                    "number" -> "Number"
                    "boolean" -> "Boolean"
                    "string" -> "String"
                    else -> itemType?.capitalize() ?: "Object"
                }
                "$prefix$mapped[] $name"
            }
            prop.`$ref` != null -> {
                val refName = refToClassName(prop.`$ref`!!)
                "$prefix$refName $name"
            }
            else -> "$prefix$kotlinType $name"
        }
    }

    private fun formatArrayField(name: String, items: Property?, requiredKeys: List<String>, prefs: Preferences): String {
        val itemType = items?.format ?: items?.type
        val mapped = when (itemType) {
            "integer" -> "Integer"
            "number" -> "Number"
            "boolean" -> "Boolean"
            "string" -> "String"
            else -> itemType?.capitalize() ?: "Object"
        }
        val prefix = if (prefs.showRequiredWithPlus && requiredKeys.contains(name)) "+" else ""
        return "$prefix$mapped[] $name"
    }

    private fun refToClassName(ref: String): String {
        var rn = ref
        val last = rn.lastIndexOf('/')
        if (last >= 0) rn = rn.substring(last + 1)
        return rn.capitalize()
    }

    private fun getClassName(schemaData: SchemaFileInfo): String =
        (schemaData.schema.title
            ?: getClassNameFromFilePath(schemaData.filename)
            ?: getClassNameFromId(schemaData.schema)).trim()

    private fun getClassNameFromFilePath(filepath: String?): String? {
        var classname = filepath
        classname?.indexOf(".")?.let { index ->
            if (index > 0) {
                classname = classname?.substring(0, index)
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
