package jsonschema_to_mermaid

import jsonschema_to_mermaid.NameSanitizer.sanitizeName
import jsonschema_to_mermaid.schema_files.SchemaFileInfo

/**
 * Processes schema definitions and converts them to diagram elements.
 */
object DefinitionProcessor {

    fun processDefinitions(
        schemaFiles: Collection<SchemaFileInfo>,
        ctx: DiagramGenerationContext
    ) {
        schemaFiles.forEach { schemaFile ->
            processSchemaDefinitions(schemaFile, ctx)
        }
    }

    private fun processSchemaDefinitions(
        schemaFile: SchemaFileInfo,
        ctx: DiagramGenerationContext
    ) {
        schemaFile.schema.definitions?.forEach { (definitionName, definitionSchema) ->
            processDefinition(definitionName, definitionSchema, ctx)
        }
    }

    private fun processDefinition(
        definitionName: String,
        definitionSchema: jsonschema_to_mermaid.jsonschema.Schema,
        ctx: DiagramGenerationContext
    ) {
        val className = sanitizeName(definitionName)
        ClassRegistry.ensureClassEntry(ctx.classProperties, className)

        definitionSchema.properties?.forEach { (propertyName, property) ->
            processDefinitionProperty(className, propertyName, property, definitionSchema, ctx)
        }
    }

    private fun processDefinitionProperty(
        className: String,
        propertyName: String,
        property: jsonschema_to_mermaid.jsonschema.Property,
        definitionSchema: jsonschema_to_mermaid.jsonschema.Schema,
        ctx: DiagramGenerationContext
    ) {
        val isRequired = definitionSchema.required.contains(propertyName)
        PropertyMapper.mapPropertyToClass(property, propertyName, className, isRequired, ctx, suppressInlineEnum = false)
        addRelationForDefinitionProperty(className, propertyName, property, ctx)
    }

    private fun addRelationForDefinitionProperty(
        className: String,
        propertyName: String,
        property: jsonschema_to_mermaid.jsonschema.Property,
        ctx: DiagramGenerationContext
    ) {
        when {
            property.`$ref` != null -> addReferenceRelation(className, propertyName, property, ctx)
            property.type == "array" && ctx.preferences.arraysAsRelation -> addArrayRelation(className, propertyName, property, ctx)
        }
    }

    private fun addReferenceRelation(
        className: String,
        propertyName: String,
        property: jsonschema_to_mermaid.jsonschema.Property,
        ctx: DiagramGenerationContext
    ) {
        val target = ClassNameResolver.refToClassName(property.`$ref`)
        val relation = RelationshipBuilder.formatRelation(className, target, null, null, propertyName, "o--")
        ctx.relations.add(relation)
    }

    private fun addArrayRelation(
        className: String,
        propertyName: String,
        property: jsonschema_to_mermaid.jsonschema.Property,
        ctx: DiagramGenerationContext
    ) {
        val items = property.items
        if (items?.`$ref` != null) {
            val target = ClassNameResolver.refToClassName(items.`$ref`)
            val relation = RelationshipBuilder.formatRelation(className, target, "1", "*", propertyName, "-->")
            ctx.relations.add(relation)
        }
    }
}

