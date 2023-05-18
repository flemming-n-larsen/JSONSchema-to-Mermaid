package jsonschema_to_mermaid

object MermaidGenerator {

    fun generate(schemas: Collection<Schema>): String {
        val strBuilder = StringBuilder()
        strBuilder.append("classDiagram\n")
        schemas.forEach { outputSchema(it, strBuilder) }
        return strBuilder.toString()
    }

    private fun outputSchema(schema: Schema, strBuilder: StringBuilder) {
        strBuilder.append("class ").append(getClassName(schema))
        if (schema.properties?.isNotEmpty() == true) {
            strBuilder.append("{")
            strBuilder.append("}")
        } else {
            strBuilder.append("\n")
        }
    }

    private fun getClassName(schema: Schema): String =
        (schema.title ?: getClassNameFromId(schema)).trim()

    private fun getClassNameFromId(schema: Schema): String {
        var className =
            schema.`$id`?.trim() ?: throw IllegalStateException("schema is missing title and \$id fields")
        val lastIndex = className.lastIndexOf("/")
        if (lastIndex >= 0) {
            className = className.substring(lastIndex + 1)
        }
        return className
    }
}