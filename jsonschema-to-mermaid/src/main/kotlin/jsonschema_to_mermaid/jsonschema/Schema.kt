package jsonschema_to_mermaid.jsonschema

data class Schema(
    val `$id`: String? = null,
    val `$schema`: String? = null,

    val title: String? = null,

    val type: String? = null,
    val properties: Map<String, Property>? = mapOf(),
    val required: List<String>? = listOf(),
    val definitions: Map<String, Schema>? = mapOf(),
    val extends: Extends? = null, // New: support for extends
    val inheritedPropertyNames: List<String>? = null,
)

// New: Extends data class to support both string and object forms
sealed class Extends {
    data class Ref(val ref: String) : Extends()
    data class Object(val ref: String) : Extends() // For future extensibility
}
