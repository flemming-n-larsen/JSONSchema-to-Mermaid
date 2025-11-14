package jsonschema_to_mermaid.schema

import jsonschema_to_mermaid.diagram.PropertyFormatter
import jsonschema_to_mermaid.diagram.ClassNameResolver
import jsonschema_to_mermaid.diagram.Preferences
import jsonschema_to_mermaid.diagram.EnumStyle
import jsonschema_to_mermaid.diagram.DiagramGenerationContext
import jsonschema_to_mermaid.diagram.NameSanitizer.sanitizeName
import jsonschema_to_mermaid.jsonschema.Property

/**
 * Responsible for mapping properties to class definitions in the diagram.
 */
object PropertyMapper {

    fun mapPropertyToClass(
        property: Property,
        propertyName: String,
        currentClassName: String,
        isRequired: Boolean,
        ctx: DiagramGenerationContext,
        suppressInlineEnum: Boolean = false
    ) {
        if (shouldHandleAsEnum(property, suppressInlineEnum, ctx.preferences)) {
            handleEnumProperty(property, propertyName, currentClassName, isRequired, ctx)
            return
        }

        handleRegularProperty(property, propertyName, currentClassName, isRequired, ctx)
    }

    private fun shouldHandleAsEnum(property: Property, suppressInlineEnum: Boolean, preferences: Preferences): Boolean {
        return property.enum != null && 
               property.enum.isNotEmpty() && 
               !(suppressInlineEnum && preferences.enumStyle == EnumStyle.INLINE)
    }

    private fun handleEnumProperty(
        property: Property,
        propertyName: String,
        currentClassName: String,
        isRequired: Boolean,
        ctx: DiagramGenerationContext
    ) {
        val targetProperties = ctx.classProperties[currentClassName]!!
        
        when (ctx.preferences.enumStyle) {
            EnumStyle.INLINE -> addInlineEnum(targetProperties, propertyName, property, isRequired)
            EnumStyle.NOTE -> addEnumAsNote(targetProperties, propertyName, property, currentClassName, isRequired, ctx)
            EnumStyle.CLASS -> addEnumAsClass(targetProperties, propertyName, property, isRequired, ctx)
        }
    }

    private fun addInlineEnum(
        targetProperties: MutableList<String>,
        propertyName: String,
        property: Property,
        isRequired: Boolean
    ) {
        targetProperties.add(PropertyFormatter.formatEnumInlineField(propertyName, property, isRequired))
    }

    private fun addEnumAsNote(
        targetProperties: MutableList<String>,
        propertyName: String,
        property: Property,
        currentClassName: String,
        isRequired: Boolean,
        ctx: DiagramGenerationContext
    ) {
        targetProperties.add(PropertyFormatter.formatField(propertyName, property, ctx.preferences, isRequired))
        val note = "$propertyName: ${property.enum?.joinToString(", ")}"
        ctx.enumNotes.add(currentClassName to note)
    }

    private fun addEnumAsClass(
        targetProperties: MutableList<String>,
        propertyName: String,
        property: Property,
        isRequired: Boolean,
        ctx: DiagramGenerationContext
    ) {
        val enumClassName = "${sanitizeName(propertyName)}Enum"
        targetProperties.add(PropertyFormatter.formatInlineField(propertyName, enumClassName, isRequired))
        ctx.enumClasses.add(enumClassName to property.enum!!)
    }

    private fun handleRegularProperty(
        property: Property,
        propertyName: String,
        currentClassName: String,
        isRequired: Boolean,
        ctx: DiagramGenerationContext
    ) {
        val targetProperties = ctx.classProperties[currentClassName]!!

        when {
            property.`$ref` != null -> handleReferenceProperty(targetProperties, propertyName, property, isRequired)
            property.type == "array" -> handleArrayProperty(targetProperties, propertyName, property, isRequired, ctx.preferences)
            property.type == "object" -> handleObjectProperty(targetProperties, propertyName, isRequired)
            else -> handlePrimitiveProperty(targetProperties, propertyName, property, isRequired, ctx.preferences)
        }
    }

    private fun handleReferenceProperty(
        targetProperties: MutableList<String>,
        propertyName: String,
        property: Property,
        isRequired: Boolean
    ) {
        targetProperties.add(
            PropertyFormatter.formatInlineField(
                propertyName, 
                ClassNameResolver.refToClassName(property.`$ref`), 
                isRequired
            )
        )
    }

    private fun handleArrayProperty(
        targetProperties: MutableList<String>,
        propertyName: String,
        property: Property,
        isRequired: Boolean,
        preferences: Preferences
    ) {
        val items = property.items
        val shouldAddAsField = !preferences.arraysAsRelation || 
                               (items?.type != "object" && items?.`$ref` == null)
        
        if (shouldAddAsField) {
            targetProperties.add(PropertyFormatter.formatArrayField(propertyName, items, isRequired))
        }
    }

    private fun handleObjectProperty(
        targetProperties: MutableList<String>,
        propertyName: String,
        isRequired: Boolean
    ) {
        val target = sanitizeName(propertyName)
        targetProperties.add(PropertyFormatter.formatInlineField(propertyName, target, isRequired))
    }

    private fun handlePrimitiveProperty(
        targetProperties: MutableList<String>,
        propertyName: String,
        property: Property,
        isRequired: Boolean,
        preferences: Preferences
    ) {
        targetProperties.add(PropertyFormatter.formatField(propertyName, property, preferences, isRequired))
    }
}
