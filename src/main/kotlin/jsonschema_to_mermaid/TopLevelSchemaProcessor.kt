package jsonschema_to_mermaid

import jsonschema_to_mermaid.schema_files.SchemaFileInfo
import jsonschema_to_mermaid.ClassNameResolver.getClassName

/**
 * Processes top-level schema properties and their relationships.
 */
object TopLevelSchemaProcessor {

    fun processTopLevelSchemas(
        schemaFiles: Collection<SchemaFileInfo>,
        ctx: DiagramGenerationContext
    ) {
        schemaFiles.forEach { schemaFile ->
            processTopLevelSchema(schemaFile, ctx)
        }
    }

    private fun processTopLevelSchema(
        schemaFile: SchemaFileInfo,
        ctx: DiagramGenerationContext
    ) {
        val className = getClassName(schemaFile)
        ClassRegistry.ensureClassEntry(ctx.classProperties, className)
        
        InheritanceHandler.handleInheritance(schemaFile, className, ctx.relations)
        
        processSchemaProperties(schemaFile, className, ctx)
    }

    private fun processSchemaProperties(
        schemaFile: SchemaFileInfo,
        className: String,
        ctx: DiagramGenerationContext
    ) {
        schemaFile.schema.properties?.forEach { (propertyName, property) ->
            processSchemaProperty(schemaFile, className, propertyName, property, ctx)
        }
    }

    private fun processSchemaProperty(
        schemaFile: SchemaFileInfo,
        className: String,
        propertyName: String,
        property: jsonschema_to_mermaid.jsonschema.Property,
        ctx: DiagramGenerationContext
    ) {
        if (InheritanceHandler.isInheritedProperty(schemaFile, propertyName)) return

        val isRequired = schemaFile.schema.required.contains(propertyName)

        when {
            CompositionKeywordHandler.handleCompositionKeywords(className, propertyName, property, ctx) -> return
            CompositionKeywordHandler.handleOneOrAnyOf(className, propertyName, property, ctx) -> return
            isMapProperty(property) -> handleMapProperty(className, propertyName, property, isRequired, ctx)
            else -> PropertyRelationshipHandler.handleTopLevelProperty(schemaFile, className, propertyName, property, isRequired, ctx)
        }
    }

    private fun isMapProperty(property: jsonschema_to_mermaid.jsonschema.Property): Boolean {
        return property.additionalProperties != null || property.patternProperties != null
    }

    private fun handleMapProperty(
        className: String,
        propertyName: String,
        property: jsonschema_to_mermaid.jsonschema.Property,
        isRequired: Boolean,
        ctx: DiagramGenerationContext
    ) {
        ctx.classProperties[className]!!.add(
            PropertyFormatter.formatField(propertyName, property, ctx.preferences, isRequired)
        )
    }
}

