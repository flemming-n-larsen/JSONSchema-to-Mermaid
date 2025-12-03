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
        irregulars[lower]?.let { return it }
        // -ies → y (companies → Company)
        if (lower.endsWith("ies") && lower.length > 3) {
            return capitalize(word.dropLast(3) + "y")
        }
        // -es endings
        if (lower.endsWith("es") && lower.length > 2) {
            val stem = word.dropLast(2)
            if (stem.endsWith("x") || stem.endsWith("z") || stem.endsWith("ch") || stem.endsWith("sh")) {
                return capitalize(stem)
            }
            if (stem.endsWith("s") && !stem.endsWith("ss")) {
                return capitalize(stem)
            }
            // For other cases like "dresses", drop only 'es'
            if (lower.endsWith("es")) {
                return capitalize(word.dropLast(2))
            }
        }
        // -s (cats → Cat)
        if (lower.endsWith("s") && lower.length > 1 && !lower.endsWith("ss")) {
            return capitalize(word.dropLast(1))
        }
        // Default: Capitalize
        return capitalize(word)
    }

    private fun capitalize(str: String): String = str.replaceFirstChar { it.uppercaseChar() }
}
