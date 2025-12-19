package jsonschema_to_mermaid

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain
import jsonschema_to_mermaid.schema_files.SchemaFilesReader
import jsonschema_to_mermaid.diagram.MermaidGenerator
import jsonschema_to_mermaid.diagram.Preferences
import jsonschema_to_mermaid.diagram.AllOfMode
import test_util.GoldenTestUtil
import test_util.resourcePath

@Suppress("unused")
class AllOfModeTest : FunSpec({

    test("allOf merge mode merges all properties into current class") {
        val schemas = SchemaFilesReader.readSchemas(setOf(resourcePath("/core/allof_merge.schema.json")))
        val mermaid = MermaidGenerator.generate(schemas, preferences = Preferences(allOfMode = AllOfMode.MERGE))
        mermaid shouldContain "class Product"
        mermaid shouldContain "String name"
        mermaid shouldContain "Number price"
        mermaid shouldContain "Number discount"
        mermaid shouldContain "String sku"
    }

    test("allOf inherit mode creates inheritance relationships") {
        val schemas = SchemaFilesReader.readSchemas(setOf(resourcePath("/core/allof_inherit.schema.json")))
        val mermaid = MermaidGenerator.generate(schemas, preferences = Preferences(allOfMode = AllOfMode.INHERIT))
        mermaid shouldContain "Person <|-- Employee"
        mermaid shouldContain "Worker <|-- Employee"
    }

    test("allOf compose mode creates composition relationships") {
        val schemas = SchemaFilesReader.readSchemas(setOf(resourcePath("/core/allof_compose.schema.json")))
        val mermaid = MermaidGenerator.generate(schemas, preferences = Preferences(allOfMode = AllOfMode.COMPOSE))
        mermaid shouldContain "Car *-- Engine"
        mermaid shouldContain "Car *-- Wheels"
    }

    test("allOf inherit mode with references produces inheritance arrows") {
        val schemas = SchemaFilesReader.readSchemas(setOf(resourcePath("/core/allof_inherit.schema.json")))
        val mermaid = MermaidGenerator.generate(schemas, preferences = Preferences(allOfMode = AllOfMode.INHERIT))
        mermaid shouldContain "Person <|-- Employee"
        mermaid shouldContain "Worker <|-- Employee"
    }

    test("allOf compose mode with references produces composition arrows") {
        val schemas = SchemaFilesReader.readSchemas(setOf(resourcePath("/core/allof_compose.schema.json")))
        val mermaid = MermaidGenerator.generate(schemas, preferences = Preferences(allOfMode = AllOfMode.COMPOSE))
        mermaid shouldContain "Car *-- Engine"
        mermaid shouldContain "Car *-- Wheels"
    }

    test("allOf merge mode is default") {
        val schemas = SchemaFilesReader.readSchemas(setOf(resourcePath("/core/allof_merge.schema.json")))
        val mermaidDefault = MermaidGenerator.generate(schemas)
        val mermaidMerge = MermaidGenerator.generate(schemas, preferences = Preferences(allOfMode = AllOfMode.MERGE))
        // Both should produce the same output - merged fields
        mermaidDefault shouldContain "class Product"
        mermaidDefault shouldContain "String name"
        mermaidMerge shouldContain "class Product"
        mermaidMerge shouldContain "String name"
    }
})

