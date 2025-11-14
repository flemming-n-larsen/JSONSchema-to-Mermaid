package jsonschema_to_mermaid.diagram

import jsonschema_to_mermaid.schema_files.SchemaFileInfo

/**
 * Converts JSON Schema primitive types to their display names in Mermaid diagrams.
 */
object TypeNameConverter {
    fun primitiveTypeName(typeOrFormat: String?): String = when (typeOrFormat) {
        "integer" -> "Integer"
        "number" -> "Number"
        "boolean" -> "Boolean"
        "string" -> "String"
        null -> "Object"
        else -> capitalizeFirst(typeOrFormat)
    }

    private fun capitalizeFirst(value: String): String =
        value.replaceFirstChar { it.uppercaseChar().toString() }
}

/**
 * Sanitizes names to be Mermaid-compatible identifiers.
 * Removes special characters and converts to PascalCase.
 */
object NameSanitizer {
    private val NON_ALPHANUMERIC_REGEX = Regex("[^A-Za-z0-9]+")

    fun sanitizeName(name: String?): String {
        if (name == null) return ""

        return name.split(NON_ALPHANUMERIC_REGEX)
            .filter { it.isNotBlank() }
            .joinToString("") { capitalizeFirst(it) }
    }

    private fun capitalizeFirst(word: String): String =
        word.replaceFirstChar { c -> c.uppercaseChar() }
}

/**
 * Resolves class names from schema files and references.
 */
object ClassNameResolver {
    fun getClassName(schemaFile: SchemaFileInfo): String {
        val fromTitle = schemaFile.schema.title?.let { NameSanitizer.sanitizeName(it) }
        if (fromTitle != null) return fromTitle

        val fromFilename = schemaFile.filename
            ?.substringBefore('.')
            ?.let { NameSanitizer.sanitizeName(it) }
        if (fromFilename != null) return fromFilename

        return "UnknownSchema"
    }

    fun refToClassName(ref: String?): String {
        if (ref == null) return "UnknownRef"

        val lastPart = extractLastRefPart(ref)
        return NameSanitizer.sanitizeName(lastPart)
    }

    private fun extractLastRefPart(ref: String): String {
        val parts = ref.split('/', '#').filter { it.isNotBlank() }
        return parts.lastOrNull() ?: ref
    }
}
