package jsonschema_to_mermaid

object MermaidGenerator {

    private val jsonFileExtRegex = Regex("(?i).json$")
    private val yamlFileExtRegex = Regex("(?i).yaml$")
    private val ymlFileExtRegex = Regex("(?i).yml$")

    fun generate(schemas: Collection<Schema>): String {
        val strBuilder = StringBuilder()
        strBuilder.append("classDiagram\n")
        schemas.forEach { outputSchema(it, strBuilder) }
        return strBuilder.toString()
    }

    private fun outputSchema(schema: Schema, strBuilder: StringBuilder) {
        strBuilder.append("class ").append(toClassName(schema.dollarId!!))
        if (schema.properties?.isNotEmpty() == true) {
            strBuilder.append("{")
            strBuilder.append("}")
        } else {
            strBuilder.append("\n")
        }
    }

    private fun toClassName(schemaId: String): String {
        var className = schemaId.trim()
        val lastIndex = schemaId.lastIndexOf("/")
        if (lastIndex >= 0) {
            className = schemaId.substring(lastIndex + 1)
        }
        className = className.replace(jsonFileExtRegex, "")
        className = className.replace(yamlFileExtRegex, "")
        className = className.replace(ymlFileExtRegex, "")
        return className
    }
}