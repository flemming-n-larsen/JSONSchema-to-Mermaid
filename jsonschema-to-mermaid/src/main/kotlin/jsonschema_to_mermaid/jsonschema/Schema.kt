package jsonschema_to_mermaid.jsonschema

data class Schema(
    val `$id`: String? = null,
    val `$schema`: String? = null,

    val title: String? = null,

    val type: String? = null,
    val properties: Map<String, Property>? = mapOf(),
    val required: List<String>? = listOf(),
    val definitions: Map<String, Schema>? = mapOf(),
)
