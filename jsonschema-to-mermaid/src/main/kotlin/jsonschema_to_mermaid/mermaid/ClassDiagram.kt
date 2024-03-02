package jsonschema_to_mermaid.mermaid

import jsonschema_to_mermaid.jsonschema.Property
import java.lang.instrument.ClassDefinition

class ClassDiagram(
    val classes: List<Property> = mutableListOf()
)