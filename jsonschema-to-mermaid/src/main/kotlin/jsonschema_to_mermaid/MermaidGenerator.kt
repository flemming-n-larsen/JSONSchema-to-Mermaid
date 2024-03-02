package jsonschema_to_mermaid

import jsonschema_to_mermaid.schema_files.SchemaFileInfo
import jsonschema_to_mermaid.jsonschema.Property
import jsonschema_to_mermaid.jsonschema.Schema

object MermaidGenerator {

    fun generate(schemas: Collection<SchemaFileInfo>): String {
        val strBuilder = StringBuilder()
        strBuilder.append("classDiagram\n")
        schemas.forEach { outputSchema(it, strBuilder) }
        return strBuilder.toString()
    }

    private fun outputSchema(schemaData: SchemaFileInfo, strBuilder: StringBuilder) {
        strBuilder.append("class ").append(getClassName(schemaData))

        val properties = schemaData.schema.properties
        if (properties?.isNotEmpty() == true) {
            strBuilder.append("{")
            properties.entries.forEach { outputProperty(name = it.key, property = it.value, strBuilder) }
            strBuilder.append("}")
        } else {
            strBuilder.append("\n")
        }
    }

    private fun outputProperty(name: String, property: Property, strBuilder: StringBuilder) {
        val type = property.format ?: property.type
        strBuilder.append("$name:$type\n")
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