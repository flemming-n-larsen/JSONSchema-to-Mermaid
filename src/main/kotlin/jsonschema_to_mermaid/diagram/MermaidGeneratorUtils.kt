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
        else -> typeOrFormat.replaceFirstChar { it.uppercaseChar() }
    }
}

/**
 * Sanitizes names to be Mermaid-compatible identifiers.
 * Removes special characters and converts to PascalCase.
 */
object NameSanitizer {
    private val NON_ALPHANUMERIC_REGEX = Regex("[^A-Za-z0-9]+")

    fun sanitizeName(name: String?): String = name
        ?.split(NON_ALPHANUMERIC_REGEX)
        ?.filter { it.isNotBlank() }
        ?.joinToString("") { it.replaceFirstChar { c -> c.uppercaseChar() } }
        ?: ""
}

/**
 * Resolves class names from schema files and references.
 */
object ClassNameResolver {
    private val nameRegistry = mutableMapOf<String, MutableList<String>>()

    fun resetRegistry() {
        nameRegistry.clear()
    }

    fun getClassName(schemaFile: SchemaFileInfo): String {
        val baseName = schemaFile.schema.title?.let { NameSanitizer.sanitizeName(it) }
            ?: schemaFile.filename?.substringBefore('.')?.let { NameSanitizer.sanitizeName(it) }
            ?: "UnknownSchema"
        val sourceId = schemaFile.filename ?: baseName
        return registerAndDisambiguate(baseName, sourceId)
    }

    fun refToClassName(ref: String?): String {
        if (ref == null) return "UnknownRef"
        val baseName = NameSanitizer.sanitizeName(extractLastRefPart(ref))
        return registerAndDisambiguate(baseName, ref)
    }

    private fun registerAndDisambiguate(baseName: String, sourceId: String): String {
        val sources = nameRegistry.getOrPut(baseName) { mutableListOf() }

        // Already registered for this source
        if (sources.contains(sourceId)) return baseName

        // Collision detected
        if (sources.isNotEmpty()) {
            val disambiguated = "${baseName}_${sources.size + 1}"
            System.err.println("[WARN] Name collision: '$baseName' used by ${sources.joinToString()} and '$sourceId'. Disambiguated to '$disambiguated'.")
            sources.add(sourceId)
            nameRegistry[disambiguated] = mutableListOf(sourceId)
            return disambiguated
        }

        sources.add(sourceId)
        return baseName
    }

    private fun extractLastRefPart(ref: String): String =
        ref.split('/', '#')
            .filter { it.isNotBlank() }
            .lastOrNull()
            ?: ref
}
