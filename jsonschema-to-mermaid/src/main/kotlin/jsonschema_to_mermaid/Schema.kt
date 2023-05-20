package jsonschema_to_mermaid

data class Schema(
    val `$id`: String? = null,
    val `$schema`: String? = null,

    val title: String? = null,

    val properties: Map<String, Any>? = mapOf()
)