package jsonschema_to_mermaid

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.shouldBe
import jsonschema_to_mermaid.schema_files.SchemaFilesReader
import jsonschema_to_mermaid.diagram.MermaidGenerator
import jsonschema_to_mermaid.diagram.Preferences
import jsonschema_to_mermaid.diagram.EnumStyle
import jsonschema_to_mermaid.diagram.RequiredFieldStyle
import test_util.GoldenTestUtil
import test_util.resourcePath

@Suppress("unused")
class MermaidGeneratorReadmeExamplesTest : FunSpec({

    test("Person JSON example generates required '+' and optional cardinality [0..1]") {
        val schemas = SchemaFilesReader.readSchemas(setOf(resourcePath("/readme_examples/person.schema.json")))
        val mermaid = MermaidGenerator.generate(schemas)
        GoldenTestUtil.assertMatchesGolden("person", mermaid)
    }

    test("Person YAML example: optional array annotated with [0..1]") {
        val schemas = SchemaFilesReader.readSchemas(setOf(resourcePath("/readme_examples/person.schema.yaml")))
        val mermaid = MermaidGenerator.generate(schemas)
        GoldenTestUtil.assertMatchesGolden("person", mermaid)
    }

    test("Order example: required vs optional markers") {
        val schemas = SchemaFilesReader.readSchemas(setOf(resourcePath("/readme_examples/order.schema.json")))
        val mermaid = MermaidGenerator.generate(schemas)
        GoldenTestUtil.assertMatchesGolden("order", mermaid)
    }

    test("ProductCatalog example retains required '+' without optional cardinality on required fields") {
        val schemas = SchemaFilesReader.readSchemas(setOf(resourcePath("/readme_examples/product-catalog.schema.yaml")))
        val mermaid = MermaidGenerator.generate(schemas, preferences = Preferences(enumStyle = EnumStyle.CLASS))
        GoldenTestUtil.assertMatchesGolden("product_catalog", mermaid)
    }

    test("Complex example: all optional fields annotated with [0..1]") {
        val schemas = SchemaFilesReader.readSchemas(setOf(resourcePath("/readme_examples/complex.schema.json")))
        val mermaid = MermaidGenerator.generate(schemas)
        GoldenTestUtil.assertMatchesGolden("complex", mermaid)
    }

    test("Extends/inheritance: optional fields show [0..1]") {
        val schemas = SchemaFilesReader.readSchemas(setOf(
            resourcePath("/readme_examples/child.schema.yaml"),
            resourcePath("/readme_examples/parent.schema.yaml")
        ))
        val mermaid = MermaidGenerator.generate(schemas)
        mermaid shouldContain "String parentField [0..1]"
        val withInherited = MermaidGenerator.generate(schemas, preferences = Preferences(showInheritedFields = true))
        withInherited shouldContain "String parentField [0..1]"
        val childCount = mermaid.lineSequence().count { it.contains("String parentField [0..1]") }
        childCount shouldBe 1
    }

    test("Transitive inheritance: optional cardinality only, no '+', uniqueness maintained") {
        val schemas = SchemaFilesReader.readSchemas(setOf(
            resourcePath("/readme_examples/grandchild.schema.yaml"),
            resourcePath("/readme_examples/child.schema.yaml"),
            resourcePath("/readme_examples/parent.schema.yaml")
        ))
        val mermaid = MermaidGenerator.generate(schemas)
        mermaid shouldContain "class Parent"
        mermaid shouldContain "class Child"
        mermaid shouldContain "class Grandchild"
        mermaid shouldContain "Parent <|-- Child"
        mermaid shouldContain "Child <|-- Grandchild"
        mermaid shouldContain "String parentField [0..1]"
        mermaid shouldContain "Integer childField [0..1]"
        mermaid shouldContain "Boolean grandchildField [0..1]"
        mermaid shouldNotContain "+String parentField"
        mermaid shouldNotContain "+Integer childField"
        mermaid shouldNotContain "+Boolean grandchildField"
        val parentFieldCount = mermaid.lineSequence().count { it.contains("String parentField [0..1]") }
        val childFieldCount = mermaid.lineSequence().count { it.contains("Integer childField [0..1]") }
        val grandchildFieldCount = mermaid.lineSequence().count { it.contains("Boolean grandchildField [0..1]") }
        parentFieldCount shouldBe 1
        childFieldCount shouldBe 1
        grandchildFieldCount shouldBe 1
    }

    test("PatternProperties: attributes field renders as Map<String,Number>") {
        val schemas = SchemaFilesReader.readSchemas(setOf(resourcePath("/pattern_properties.schema.json")))
        val mermaid = MermaidGenerator.generate(schemas)
        mermaid shouldContain "class PatternPropertiesExample"
        // Only the first pattern is used for type inference (should be Number)
        mermaid shouldContain "Map~String, Number~ attributes"
        // Should not render Map<String,Boolean> (second pattern is ignored)
        mermaid shouldNotContain "Map<String,Boolean>"
    }

    test("noClassDiagramHeader omits classDiagram header") {
        val schemas = SchemaFilesReader.readSchemas(setOf(resourcePath("/readme_examples/person.schema.json")))
        val mermaid = MermaidGenerator.generate(schemas, noClassDiagramHeader = true)
        mermaid shouldNotContain "classDiagram"
        mermaid shouldContain "class Person"
        mermaid shouldContain "+Integer id"
        mermaid shouldContain "+String name"
    }

    test("Enum example renders enum inline by default") {
        val schemas = SchemaFilesReader.readSchemas(setOf(resourcePath("/readme_examples/enum-example.schema.json")))
        val mermaid = MermaidGenerator.generate(schemas, preferences = Preferences(enumStyle = EnumStyle.INLINE))
        mermaid shouldContain "{A|B|C} status"
    }

    test("Enum example renders enum as note when enumStyle=NOTE") {
        val schemas = SchemaFilesReader.readSchemas(setOf(resourcePath("/readme_examples/enum-example.schema.json")))
        val mermaid = MermaidGenerator.generate(schemas, preferences = Preferences(enumStyle = EnumStyle.NOTE))
        mermaid shouldContain "note for EnumExample \"status: A, B, C\""
    }

    test("Enum example renders enum as class when enumStyle=CLASS") {
        val schemas = SchemaFilesReader.readSchemas(setOf(resourcePath("/readme_examples/enum-example.schema.json")))
        val mermaid = MermaidGenerator.generate(schemas, preferences = Preferences(enumStyle = EnumStyle.CLASS))
        GoldenTestUtil.assertMatchesGolden("enum-example", mermaid)
    }

    test("Name collision: two schemas with same title produce distinct class names") {
        val schemas = SchemaFilesReader.readSchemas(setOf(
            resourcePath("/readme_examples/collision-a.schema.json"),
            resourcePath("/readme_examples/collision-b.schema.json")
        ))
        val mermaid = MermaidGenerator.generate(schemas)
        // Should contain both Product and Product_2
        mermaid shouldContain "class Product "
        mermaid shouldContain "class Product_2 "
        // Should not contain a third variant
        mermaid shouldNotContain "class Product_3 "
    }

    test("Order example renders arrays inline when arraysAsRelation=false") {
        val schemas = SchemaFilesReader.readSchemas(setOf(resourcePath("/readme_examples/order.schema.json")))
        val mermaid = MermaidGenerator.generate(schemas, preferences = Preferences(arraysAsRelation = false))
        GoldenTestUtil.assertMatchesGolden("order_arrays_inline", mermaid)
    }

    test("Person example renders without markers when required-style=none") {
        val schemas = SchemaFilesReader.readSchemas(setOf(resourcePath("/readme_examples/person.schema.json")))
        val mermaid = MermaidGenerator.generate(schemas, preferences = Preferences(requiredFieldStyle = RequiredFieldStyle.NONE))
        GoldenTestUtil.assertMatchesGolden("person_required_none", mermaid)
    }

    test("Person example renders suffix-q optional markers") {
        val schemas = SchemaFilesReader.readSchemas(setOf(resourcePath("/readme_examples/person.schema.json")))
        val mermaid = MermaidGenerator.generate(schemas, preferences = Preferences(requiredFieldStyle = RequiredFieldStyle.SUFFIX_Q))
        GoldenTestUtil.assertMatchesGolden("person_required_suffix_q", mermaid)
    }
})
