package jsonschema_to_mermaid

data class Schema(
    var `$id`: String? = null,
    var `$schema`: String? = null,

    var title: String? = null,

    var properties: Map<String, Any>? = mapOf()
)