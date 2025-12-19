package jsonschema_to_mermaid.diagram

import jsonschema_to_mermaid.diagram.TypeNameConverter.primitiveTypeName
import jsonschema_to_mermaid.diagram.ClassNameResolver.refToClassName
import jsonschema_to_mermaid.jsonschema.Property

/**
 * Responsible for formatting properties into Mermaid field representations.
 */
object PropertyFormatter {

    fun formatEnumInlineField(propertyName: String, property: Property, isRequired: Boolean, preferences: Preferences): String =
        formatFieldWithMultiplicity("{${property.enum?.joinToString("|") ?: ""}}", propertyName, isRequired, preferences)

    fun formatInlineField(propertyName: String, refType: String, isRequired: Boolean, preferences: Preferences): String =
        formatFieldWithMultiplicity(refType, propertyName, isRequired, preferences)

    fun formatArrayField(propertyName: String, items: Property?, isRequired: Boolean, preferences: Preferences): String {
        val itemType = items?.type ?: items?.format
        val mapped = primitiveTypeName(itemType)
        val singularName = if (preferences.useEnglishSingularizer)
            EnglishSingularizer.toSingular(propertyName)
        else
            propertyName.replaceFirstChar { it.uppercaseChar() }
        return formatFieldWithMultiplicity("$mapped[]", singularName, isRequired, preferences)
    }

    fun formatField(propertyName: String, property: Property?, preferences: Preferences, isRequired: Boolean = false): String {
        return when {
            property?.additionalProperties != null -> formatAdditionalPropertiesField(propertyName, property, isRequired, preferences)
            property?.patternProperties != null -> formatPatternPropertiesField(propertyName, property, isRequired, preferences)
            property == null -> formatPrimitiveField(propertyName, null, isRequired, preferences)
            property.type == "array" && property.items != null && !preferences.arraysAsRelation -> formatArrayPropertyField(propertyName, property, isRequired, preferences)
            property.`$ref` != null -> formatReferenceField(propertyName, property, isRequired, preferences)
            else -> formatPrimitiveField(propertyName, property, isRequired, preferences)
        }
    }

    private fun formatAdditionalPropertiesField(propertyName: String, property: Property, isRequired: Boolean, preferences: Preferences): String {
        val mapped = extractAdditionalPropertiesType(property.additionalProperties)
        return formatFieldWithMultiplicity("Map~String, $mapped~", propertyName, isRequired, preferences)
    }

    private fun extractAdditionalPropertiesType(additionalProperties: Any?): String =
        (additionalProperties as? Map<*, *>)
            ?.get("type")
            ?.let { primitiveTypeName(it as? String) }
            ?: "Object"

    private fun formatPatternPropertiesField(propertyName: String, property: Property, isRequired: Boolean, preferences: Preferences): String {
        val mapped = extractPatternPropertiesType(property.patternProperties)
        return formatFieldWithMultiplicity("Map~String, $mapped~", propertyName, isRequired, preferences)
    }

    private fun extractPatternPropertiesType(patternProperties: Map<String, Property>?): String =
        patternProperties?.entries?.firstOrNull()?.value?.let { patternProp ->
            primitiveTypeName(patternProp.type ?: patternProp.format)
        } ?: "Object"

    private fun formatArrayPropertyField(propertyName: String, property: Property, isRequired: Boolean, preferences: Preferences): String {
        val itemType = property.items?.type ?: property.items?.format
        val mapped = primitiveTypeName(itemType)
        val singularName = if (preferences.useEnglishSingularizer) EnglishSingularizer.toSingular(propertyName) else propertyName.replaceFirstChar { it.uppercaseChar() }
        return formatFieldWithMultiplicity("$mapped[]", singularName, isRequired, preferences)
    }

    private fun formatReferenceField(propertyName: String, property: Property, isRequired: Boolean, preferences: Preferences): String {
        val refName = refToClassName(property.`$ref`)
        return formatFieldWithMultiplicity(refName, propertyName, isRequired, preferences)
    }

    private fun formatPrimitiveField(propertyName: String, property: Property?, isRequired: Boolean, preferences: Preferences): String {
        val type = property?.type ?: property?.format
        val kotlinType = primitiveTypeName(type)
        return formatFieldWithMultiplicity(kotlinType, propertyName, isRequired, preferences)
    }

    private fun formatFieldWithMultiplicity(type: String, propertyName: String, isRequired: Boolean, preferences: Preferences): String {
        val adjustedName = when {
            !isRequired && preferences.requiredFieldStyle == RequiredFieldStyle.SUFFIX_Q -> "$propertyName?"
            else -> propertyName
        }
        return when {
            isRequired && preferences.requiredFieldStyle == RequiredFieldStyle.PLUS -> "+$type $adjustedName"
            isRequired -> "$type $adjustedName"
            preferences.requiredFieldStyle in setOf(RequiredFieldStyle.NONE, RequiredFieldStyle.SUFFIX_Q) -> "$type $adjustedName"
            else -> "$type $adjustedName [0..1]"
        }
    }
}
