package jsonschema_to_mermaid.diagram

/**
 * Enum rendering style for Mermaid diagrams.
 */
enum class EnumStyle {
    INLINE, // {A|B|C}
    NOTE,   // Mermaid note
    CLASS   // Separate <<enumeration>> class
}

/**
 * Preferences for customizing Mermaid diagram generation.
 */
data class Preferences(
    val showRequiredWithPlus: Boolean = true,
    val arraysAsRelation: Boolean = true,
    val enumStyle: EnumStyle = EnumStyle.INLINE,
)

/**
 * Context for diagram generation, encapsulating shared state and collections.
 */
data class DiagramGenerationContext(
    val classProperties: MutableMap<String, MutableList<String>>,
    val relations: MutableList<String>,
    val preferences: Preferences,
    val enumNotes: MutableList<Pair<String, String>>,
    val enumClasses: MutableList<Pair<String, List<String>>>
)
