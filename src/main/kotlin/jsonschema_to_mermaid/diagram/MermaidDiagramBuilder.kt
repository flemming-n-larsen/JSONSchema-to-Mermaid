package jsonschema_to_mermaid.diagram

import jsonschema_to_mermaid.schema_files.SchemaFileInfo
import jsonschema_to_mermaid.relationship.InheritanceHandler
import jsonschema_to_mermaid.schema.DefinitionProcessor
import jsonschema_to_mermaid.schema.TopLevelSchemaProcessor

/**
 * Handles the main logic for building Mermaid diagrams from JSON Schema files.
 * This is the main entry point that coordinates all diagram generation activities.
 */
object MermaidDiagramBuilder {
    fun build(
        schemaFiles: Collection<SchemaFileInfo>,
        noClassDiagramHeader: Boolean,
        preferences: Preferences
    ): String {
        // Reset name registry for each diagram build
        ClassNameResolver.resetRegistry()
        InheritanceHandler.setLoadedSchemas(schemaFiles.toList())
        val ctx = createDiagramContext(preferences)
        DefinitionProcessor.processDefinitions(schemaFiles, ctx)
        TopLevelSchemaProcessor.processTopLevelSchemas(schemaFiles, ctx)
        return DiagramOutputBuilder.buildOutput(
            ctx.classProperties,
            ctx.relations,
            noClassDiagramHeader,
            preferences,
            ctx.enumNotes,
            ctx.enumClasses
        )
    }
    private fun createDiagramContext(preferences: Preferences): DiagramGenerationContext {
        return DiagramGenerationContext(
            classProperties = linkedMapOf(),
            relations = mutableListOf(),
            preferences = preferences,
            enumNotes = mutableListOf(),
            enumClasses = mutableListOf()
        )
    }
}
