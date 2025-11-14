package jsonschema_to_mermaid.diagram

import jsonschema_to_mermaid.diagram.TypeNameConverter.primitiveTypeName
import jsonschema_to_mermaid.diagram.ClassNameResolver.refToClassName
import jsonschema_to_mermaid.jsonschema.Property

/**
 * Responsible for formatting properties into Mermaid field representations.
 */
object PropertyFormatter {

    fun formatEnumInlineField(propertyName: String, property: Property, isRequired: Boolean): String {
        val enumVals = property.enum?.joinToString("|") ?: ""
        val typeStr = "{$enumVals}"
        return formatFieldWithMultiplicity(typeStr, propertyName, isRequired)
    }

    fun formatInlineField(propertyName: String, refType: String, isRequired: Boolean): String {
        return formatFieldWithMultiplicity(refType, propertyName, isRequired)
    }

    fun formatArrayField(propertyName: String, items: Property?, isRequired: Boolean): String {
        val itemType = items?.type ?: items?.format
        val mapped = primitiveTypeName(itemType)
        return formatFieldWithMultiplicity("$mapped[]", propertyName, isRequired)
    }

    fun formatField(propertyName: String, property: Property?, preferences: Preferences, isRequired: Boolean = false): String {
        return when {
            property?.additionalProperties != null -> formatAdditionalPropertiesField(propertyName, property, isRequired)
            property?.patternProperties != null -> formatPatternPropertiesField(propertyName, property, isRequired)
            property == null -> formatPrimitiveField(propertyName, null, isRequired)
            property.type == "array" && property.items != null && !preferences.arraysAsRelation -> formatArrayPropertyField(propertyName, property, isRequired)
            property.`$ref` != null -> formatReferenceField(propertyName, property, isRequired)
            else -> formatPrimitiveField(propertyName, property, isRequired)
        }
    }

    private fun formatAdditionalPropertiesField(propertyName: String, property: Property, isRequired: Boolean): String {
        val mapped = extractAdditionalPropertiesType(property.additionalProperties)
        return formatFieldWithMultiplicity("Map~String, $mapped~", propertyName, isRequired)
    }

    private fun extractAdditionalPropertiesType(additionalProperties: Any?): String {
        return if (additionalProperties is Map<*, *>) {
            val type = additionalProperties["type"] as? String
            primitiveTypeName(type)
        } else {
            "Object"
        }
    }

    private fun formatPatternPropertiesField(propertyName: String, property: Property, isRequired: Boolean): String {
        val mapped = extractPatternPropertiesType(property.patternProperties)
        return formatFieldWithMultiplicity("Map~String, $mapped~", propertyName, isRequired)
    }

    private fun extractPatternPropertiesType(patternProperties: Map<String, Property>?): String {
        val firstPattern = patternProperties?.entries?.firstOrNull()
        return if (firstPattern != null) {
            val patternProp = firstPattern.value
            primitiveTypeName(patternProp.type ?: patternProp.format)
        } else {
            "Object"
        }
    }

    private fun formatArrayPropertyField(propertyName: String, property: Property, isRequired: Boolean): String {
        val itemType = property.items?.type ?: property.items?.format
        val mapped = primitiveTypeName(itemType)
        return formatFieldWithMultiplicity("$mapped[]", propertyName, isRequired)
    }

    private fun formatReferenceField(propertyName: String, property: Property, isRequired: Boolean): String {
        val refName = refToClassName(property.`$ref`)
        return formatFieldWithMultiplicity(refName, propertyName, isRequired)
    }

    private fun formatPrimitiveField(propertyName: String, property: Property?, isRequired: Boolean): String {
        val type = property?.type ?: property?.format
        val kotlinType = primitiveTypeName(type)
        return formatFieldWithMultiplicity(kotlinType, propertyName, isRequired)
    }

    private fun formatFieldWithMultiplicity(type: String, propertyName: String, isRequired: Boolean): String {
        return if (isRequired) {
            "+$type $propertyName"
        } else {
            "$type $propertyName [0..1]"
        }
    }
}
