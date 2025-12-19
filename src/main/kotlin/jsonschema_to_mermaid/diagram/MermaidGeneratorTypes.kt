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
 * Controls how required vs optional fields are annotated.
 */
enum class RequiredFieldStyle {
    PLUS,     // Prefix required fields with '+'
    NONE,     // Do not mark required vs optional
    SUFFIX_Q  // Append '?' to optional fields
}

/**
 * Controls how allOf composition is visualized in Mermaid diagrams.
 */
enum class AllOfMode {
    MERGE,    // Merge all object fields into the current class (default)
    INHERIT,  // Treat each object in allOf as a superclass (draw inheritance arrows)
    COMPOSE   // Treat each object in allOf as an aggregation/association (draw composition arrows)
}

/**
 * Preferences for customizing Mermaid diagram generation.
 */
data class Preferences(
    val arraysAsRelation: Boolean = true,
    val enumStyle: EnumStyle = EnumStyle.INLINE,
    val useEnglishSingularizer: Boolean = true, // New option for array item naming
    val showInheritedFields: Boolean = false,
    val requiredFieldStyle: RequiredFieldStyle = RequiredFieldStyle.PLUS,
    val allOfMode: AllOfMode = AllOfMode.MERGE
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
