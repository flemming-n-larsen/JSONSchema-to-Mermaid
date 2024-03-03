package jsonschema_to_mermaid

import jsonschema_to_mermaid.mermaid.ClassDefinition
import jsonschema_to_mermaid.mermaid.ClassDiagram
import jsonschema_to_mermaid.mermaid.Property
import net.pwall.json.schema.JSONSchema
import net.pwall.json.schema.subschema.PropertiesSchema
import java.nio.file.Path
import kotlin.io.path.pathString

object MermaidClassDiagramGenerator {

    fun generate(paths: Collection<Path>) =
        ClassDiagram(
            classes = paths.map { JSONSchema.parseFile(it.pathString) }.map { generateClass(it as JSONSchema.General) }.toList()
        )

    private fun generateClass(schema: JSONSchema.General) =
        ClassDefinition(
            title = schema.title ?: "[no name]",
            schema.children.filterIsInstance { PropertiesSchema.class }
//            properties = schema. properties?.map { generateProperty(it.key, it.value) }?.toList() ?: listOf()
        )
    /*
        private fun generateProperty(name: String, property: jsonschema_to_mermaid.jsonschema.Property) =
            Property(
                name = name,
                type = property.type.orEmpty()
            )
    */
}