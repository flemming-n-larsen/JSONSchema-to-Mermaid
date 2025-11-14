package jsonschema_to_mermaid

import jsonschema_to_mermaid.schema_files.SchemaFileInfo

object MermaidGeneratorUtils {
    fun primitiveTypeName(typeOrFormat: String?): String = when (typeOrFormat) {
        "integer" -> "Integer"
        "number" -> "Number"
        "boolean" -> "Boolean"
        "string" -> "String"
        null -> "Object"
        else -> typeOrFormat.replaceFirstChar { it.uppercaseChar().toString() }
    }

    fun sanitizeName(name: String?): String = name?.split(Regex("[^A-Za-z0-9]+")).orEmpty()
        .filter { it.isNotBlank() }
        .joinToString("") { it.replaceFirstChar { c -> c.uppercaseChar() } }

    fun getClassName(schemaFile: SchemaFileInfo): String =
        schemaFile.schema.title?.let { sanitizeName(it) }
            ?: schemaFile.filename?.substringBefore('.')?.let { sanitizeName(it) }
            ?: "UnknownSchema"

    fun refToClassName(ref: String?): String {
        if (ref == null) return "UnknownRef"
        val parts = ref.split('/', '#').filter { it.isNotBlank() }
        return sanitizeName(parts.lastOrNull() ?: ref)
    }
}

