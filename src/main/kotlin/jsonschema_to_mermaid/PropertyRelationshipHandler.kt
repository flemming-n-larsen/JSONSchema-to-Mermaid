package jsonschema_to_mermaid

import jsonschema_to_mermaid.NameSanitizer.sanitizeName
import jsonschema_to_mermaid.ClassNameResolver.refToClassName
import jsonschema_to_mermaid.ClassNameResolver.getClassName
import jsonschema_to_mermaid.jsonschema.Property
import jsonschema_to_mermaid.schema_files.SchemaFileInfo

/**
 * Responsible for handling property relationships and complex property types.
 */
object PropertyRelationshipHandler {

    fun handleTopLevelProperty(
        schemaFile: SchemaFileInfo,
        className: String,
        propertyName: String,
        property: Property,
        isRequired: Boolean,
        ctx: DiagramGenerationContext
    ) {
        when {
            property.`$ref` != null -> handleReferenceRelation(className, propertyName, property, isRequired, ctx)
            property.type == "array" && ctx.preferences.arraysAsRelation -> handleArrayRelation(schemaFile, className, propertyName, property, isRequired, ctx)
            property.type == "object" -> handleObjectRelation(className, propertyName, property, isRequired, ctx)
            else -> PropertyMapper.mapPropertyToClass(property, propertyName, className, isRequired, ctx)
        }
    }

    private fun handleReferenceRelation(
        className: String,
        propertyName: String,
        property: Property,
        isRequired: Boolean,
        ctx: DiagramGenerationContext
    ) {
        val multiplicity = if (isRequired) "1" else "0..1"
        val relation = RelationshipBuilder.formatRelation(
            className,
            refToClassName(property.`$ref`),
            multiplicity,
            "1",
            propertyName,
            "-->"
        )
        ctx.relations.add(relation)
    }

    private fun handleArrayRelation(
        schemaFile: SchemaFileInfo,
        className: String,
        propertyName: String,
        property: Property,
        isRequired: Boolean,
        ctx: DiagramGenerationContext
    ) {
        val items = property.items

        when {
            items?.`$ref` != null -> addArrayReferenceRelation(className, propertyName, items, ctx)
            items?.type == "object" -> createInlineObjectForArray(schemaFile, className, propertyName, items, ctx)
            else -> addArrayAsField(className, propertyName, items, isRequired, ctx)
        }
    }

    private fun addArrayReferenceRelation(
        className: String,
        propertyName: String,
        items: Property,
        ctx: DiagramGenerationContext
    ) {
        val relation = RelationshipBuilder.formatRelation(
            className,
            refToClassName(items.`$ref`),
            "1",
            "*",
            propertyName,
            "-->"
        )
        ctx.relations.add(relation)
    }

    private fun createInlineObjectForArray(
        schemaFile: SchemaFileInfo,
        className: String,
        propertyName: String,
        items: Property,
        ctx: DiagramGenerationContext
    ) {
        val targetClassName = generateArrayItemClassName(schemaFile, className, propertyName)
        ClassRegistry.ensureClassEntry(ctx.classProperties, targetClassName)

        processInlineObjectProperties(items, targetClassName, ctx)

        val relation = RelationshipBuilder.formatRelation(className, targetClassName, "1", "*", propertyName, "-->")
        ctx.relations.add(relation)
    }

    private fun generateArrayItemClassName(schemaFile: SchemaFileInfo, className: String, propertyName: String): String {
        val base = if (propertyName.endsWith("s")) propertyName.dropLast(1) else propertyName
        val parent = className.trim().ifEmpty { ClassNameResolver.getClassName(schemaFile) }
        return parent + sanitizeName(base).replaceFirstChar { it.uppercaseChar() }
    }

    private fun processInlineObjectProperties(items: Property, targetClassName: String, ctx: DiagramGenerationContext) {
        val itemProperties = items.properties ?: emptyMap()
        itemProperties.forEach { (innerPropertyName, innerProperty) ->
            val subRequired = items.required.contains(innerPropertyName)
            PropertyMapper.mapPropertyToClass(innerProperty, innerPropertyName, targetClassName, subRequired, ctx)
        }
    }

    private fun addArrayAsField(
        className: String,
        propertyName: String,
        items: Property?,
        isRequired: Boolean,
        ctx: DiagramGenerationContext
    ) {
        ctx.classProperties[className]!!.add(PropertyFormatter.formatArrayField(propertyName, items, isRequired))
    }

    fun handleObjectRelation(
        className: String,
        propertyName: String,
        property: Property,
        isRequired: Boolean,
        ctx: DiagramGenerationContext
    ) {
        val target = sanitizeName(propertyName)
        ClassRegistry.ensureClassEntry(ctx.classProperties, target)

        processObjectProperties(property, target, ctx)

        val multiplicity = if (isRequired) "1" else "0..1"
        val relation = RelationshipBuilder.formatRelation(className, target, multiplicity, "1", propertyName, "-->")
        ctx.relations.add(relation)
    }

    private fun processObjectProperties(property: Property, targetClassName: String, ctx: DiagramGenerationContext) {
        val subProperties = property.properties ?: emptyMap()
        subProperties.forEach { (innerPropertyName, innerProperty) ->
            val subRequired = property.required.contains(innerPropertyName)
            PropertyMapper.mapPropertyToClass(innerProperty, innerPropertyName, targetClassName, subRequired, ctx)
        }
    }
}

