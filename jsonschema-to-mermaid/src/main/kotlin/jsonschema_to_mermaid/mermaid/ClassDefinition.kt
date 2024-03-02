package jsonschema_to_mermaid.mermaid

class ClassDefinition(
    val title: String,
    val properties: List<Property> = mutableListOf()
)