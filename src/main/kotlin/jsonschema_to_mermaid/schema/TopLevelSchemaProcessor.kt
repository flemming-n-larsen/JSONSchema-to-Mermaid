package jsonschema_to_mermaid.schema

import jsonschema_to_mermaid.schema_files.SchemaFileInfo
import jsonschema_to_mermaid.diagram.ClassNameResolver
import jsonschema_to_mermaid.diagram.PropertyFormatter
import jsonschema_to_mermaid.diagram.DiagramGenerationContext
import jsonschema_to_mermaid.relationship.InheritanceHandler
import jsonschema_to_mermaid.relationship.CompositionKeywordHandler
import jsonschema_to_mermaid.relationship.PropertyRelationshipHandler

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
        val className = ClassNameResolver.getClassName(schemaFile)
        ClassRegistry.ensureClassEntry(ctx.classProperties, className)

        InheritanceHandler.handleInheritance(schemaFile, className, ctx.relations)

        // Handle allOf at the schema level (for composition patterns)
        if (!schemaFile.schema.allOf.isNullOrEmpty()) {
            handleSchemaLevelAllOf(schemaFile, className, ctx)
        }

        processSchemaProperties(schemaFile, className, ctx)
    }

    private fun handleSchemaLevelAllOf(
        schemaFile: SchemaFileInfo,
        className: String,
        ctx: DiagramGenerationContext
    ) {
        // Create a synthetic property to represent the schema-level allOf
        val syntheticProperty = jsonschema_to_mermaid.jsonschema.Property(
            allOf = schemaFile.schema.allOf
        )
        CompositionKeywordHandler.handleCompositionKeywords(className, "", syntheticProperty, ctx)
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
        if (InheritanceHandler.shouldSkipInheritedProperty(schemaFile, propertyName)) return

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
