package jsonschema_to_mermaid

import jsonschema_to_mermaid.mermaid.ClassDefinition
import jsonschema_to_mermaid.mermaid.ClassDiagram
import jsonschema_to_mermaid.mermaid.Property
import jsonschema_to_mermaid.schema_files.SchemaFileInfo

object MermaidClassDiagramGenerator {

    fun generate(schemas: List<SchemaFileInfo>) = ClassDiagram(
        classes = schemas.map { generateClass(it.schema) }.toList()
    )

    private fun generateClass(schema: jsonschema_to_mermaid.jsonschema.Schema) = ClassDefinition(
        title = schema.title ?: "[no name]",
        properties = schema.properties?.map { generateProperty(it.key, it.value) }?.toList() ?: listOf()
    )

    private fun generateProperty(name: String, property: jsonschema_to_mermaid.jsonschema.Property) = Property(
        name = name,
        type = property.type.orEmpty()
    )
}