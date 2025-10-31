package jsonschema_to_mermaid.jsonschema

// Added `required` to support inline object required lists.
data class Property(
    val type: String? = null,
    val format: String? = null,
    val items: Property? = null,
    val `$ref`: String? = null,
    val properties: Map<String, Property>? = mapOf(),
    val enum: List<String>? = null,
    val additionalProperties: Any? = null,
    val allOf: List<Property>? = null,
    val oneOf: List<Property>? = null,
    val anyOf: List<Property>? = null,
    val required: List<String>? = null, // NEW
)
