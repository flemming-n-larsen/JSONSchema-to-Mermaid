package jsonschema_to_mermaid.diagram

/**
 * Utility for converting plural property names to singular for array item naming.
 * Handles common English pluralization rules and edge cases.
 */
object EnglishSingularizer {
    private val irregulars = mapOf(
        "children" to "Child",
        "mice" to "Mouse",
        "geese" to "Goose",
        "men" to "Man",
        "women" to "Woman",
        "teeth" to "Tooth",
        "feet" to "Foot",
        "data" to "Datum",
        "people" to "Person"
    )

    fun toSingular(word: String): String {
        val lower = word.lowercase()

        // Check irregulars first
        irregulars[lower]?.let { return it }

        return when {
            // -ies → y (companies → Company)
            lower.endsWith("ies") && lower.length > 3 ->
                capitalize(word.dropLast(3) + "y")

            // -es endings for x, z, ch, sh
            lower.endsWith("es") && lower.length > 2 ->
                word.dropLast(2).let { stem ->
                    when {
                        endsWithAny(stem, "x", "z", "ch", "sh") -> capitalize(stem)
                        stem.endsWith("s") && !stem.endsWith("ss") -> capitalize(stem)
                        else -> capitalize(stem)
                    }
                }

            // -s (cats → Cat)
            lower.endsWith("s") && lower.length > 1 && !lower.endsWith("ss") ->
                capitalize(word.dropLast(1))

            // Default: Capitalize
            else -> capitalize(word)
        }
    }

    private fun capitalize(str: String): String =
        str.replaceFirstChar { it.uppercaseChar() }

    private fun endsWithAny(str: String, vararg suffixes: String): Boolean =
        suffixes.any { str.endsWith(it) }
}
