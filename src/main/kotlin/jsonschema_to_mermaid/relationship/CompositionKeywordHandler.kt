package jsonschema_to_mermaid.relationship

import jsonschema_to_mermaid.diagram.NameSanitizer
import jsonschema_to_mermaid.diagram.ClassNameResolver
import jsonschema_to_mermaid.schema.PropertyMapper
import jsonschema_to_mermaid.diagram.PropertyFormatter
import jsonschema_to_mermaid.diagram.DiagramGenerationContext
import jsonschema_to_mermaid.schema.ClassRegistry
import jsonschema_to_mermaid.relationship.RelationshipBuilder
import jsonschema_to_mermaid.jsonschema.Property

/**
 * Handles JSON Schema composition keywords (allOf, oneOf, anyOf).
 */
object CompositionKeywordHandler {

    fun handleCompositionKeywords(
        className: String,
        propertyName: String,
        property: Property,
        ctx: DiagramGenerationContext
    ): Boolean {
        return handleAllOf(className, propertyName, property, ctx)
    }

    fun handleOneOrAnyOf(
        className: String,
        propertyName: String,
        property: Property,
        ctx: DiagramGenerationContext
    ): Boolean {
        return handleOneOf(className, propertyName, property, ctx) ||
               handleAnyOf(className, propertyName, property, ctx)
    }

    private fun handleAllOf(
        className: String,
        propertyName: String,
        property: Property,
        ctx: DiagramGenerationContext
    ): Boolean {
        if (property.allOf.isNullOrEmpty()) return false

        val refs = property.allOf.filter { it.`$ref` != null }
        val inlines = property.allOf.filter { it.`$ref` == null && it.properties != null }

        var handled = false
        if (refs.isNotEmpty()) {
            processAllOfReferences(className, propertyName, refs, ctx)
            handled = true
        }
        if (inlines.isNotEmpty()) {
            processAllOfInlines(className, inlines, ctx)
            handled = true
        }
        return handled
    }

    private fun processAllOfReferences(
        className: String,
        propertyName: String,
        refs: List<Property>,
        ctx: DiagramGenerationContext
    ) {
        refs.forEach { ref ->
            val relation = RelationshipBuilder.formatRelation(
                className,
                ClassNameResolver.refToClassName(ref.`$ref`),
                "1",
                "1",
                propertyName,
                "-->"
            )
            ctx.relations.add(relation)
        }
    }

    private fun processAllOfInlines(
        className: String,
        inlines: List<Property>,
        ctx: DiagramGenerationContext
    ) {
        inlines.forEach { inline ->
            inline.properties?.forEach { (inlinePropName, inlineProp) ->
                val isRequired = inline.required.contains(inlinePropName)
                PropertyMapper.mapPropertyToClass(inlineProp, inlinePropName, className, isRequired, ctx)
            }
        }
    }

    private fun handleOneOf(
        className: String,
        propertyName: String,
        property: Property,
        ctx: DiagramGenerationContext
    ): Boolean {
        return processAlternatives(className, propertyName, property.oneOf, "oneOf", ctx)
    }

    private fun handleAnyOf(
        className: String,
        propertyName: String,
        property: Property,
        ctx: DiagramGenerationContext
    ): Boolean {
        return processAlternatives(className, propertyName, property.anyOf, "anyOf", ctx)
    }

    private fun processAlternatives(
        className: String,
        propertyName: String,
        members: List<Property>?,
        label: String,
        ctx: DiagramGenerationContext
    ): Boolean {
        if (members.isNullOrEmpty()) return false

        members.forEach { member ->
            processAlternativeMember(className, propertyName, member, label, ctx)
        }
        return true
    }

    private fun processAlternativeMember(
        className: String,
        propertyName: String,
        member: Property,
        label: String,
        ctx: DiagramGenerationContext
    ) {
        when {
            member.`$ref` != null -> addAlternativeReference(className, propertyName, member, label, ctx)
            member.type == "object" -> createAlternativeObject(className, propertyName, member, label, ctx)
        }
    }

    private fun addAlternativeReference(
        className: String,
        propertyName: String,
        member: Property,
        label: String,
        ctx: DiagramGenerationContext
    ) {
        val relation = RelationshipBuilder.formatRelation(
            className,
            ClassNameResolver.refToClassName(member.`$ref`),
            "1",
            "1",
            "$propertyName ($label)",
            "-->"
        )
        ctx.relations.add(relation)
    }

    private fun createAlternativeObject(
        className: String,
        propertyName: String,
        member: Property,
        label: String,
        ctx: DiagramGenerationContext
    ) {
        val target = NameSanitizer.sanitizeName(propertyName) + "-option"
        ClassRegistry.ensureClassEntry(ctx.classProperties, target)

        val subProperties = member.properties ?: emptyMap()
        subProperties.forEach { (innerPropertyName, innerProperty) ->
            val subRequired = member.required.contains(innerPropertyName)
            PropertyMapper.mapPropertyToClass(innerProperty, innerPropertyName, target, subRequired, ctx)
        }

        val relation = RelationshipBuilder.formatRelation(
            className,
            target,
            "1",
            "1",
            "$propertyName ($label)",
            "-->"
        )
        ctx.relations.add(relation)
    }
}
