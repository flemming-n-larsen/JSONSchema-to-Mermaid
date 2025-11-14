package jsonschema_to_mermaid

import jsonschema_to_mermaid.schema_files.SchemaFileInfo
import jsonschema_to_mermaid.Preferences
import jsonschema_to_mermaid.EnumStyle
import jsonschema_to_mermaid.DiagramGenerationContext
import jsonschema_to_mermaid.MermaidDiagramBuilder

/**
 * Generates Mermaid class diagrams from JSON Schema files.
 */
object MermaidGenerator {
    /**
     * Generate a Mermaid class diagram from a collection of schema files.
     * @param schemaFiles The schema files to process.
     * @param preferences Preferences for diagram generation.
     * @return Mermaid class diagram as a string.
     */
    fun generate(
        schemaFiles: Collection<SchemaFileInfo>,
        noClassDiagramHeader: Boolean = false,
        preferences: Preferences = Preferences()
    ): String {
        return MermaidDiagramBuilder.build(schemaFiles, noClassDiagramHeader, preferences)
    }
}
