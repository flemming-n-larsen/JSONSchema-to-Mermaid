package jsonschema_to_mermaid

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.shouldBe
import jsonschema_to_mermaid.schema_files.SchemaFilesReader
import test_util.resourcePath

@Suppress("unused")
class MermaidGeneratorReadmeExamplesTest : FunSpec({

    test("Person JSON example generates required '+' and optional cardinality [0..1]") {
        val schemas = SchemaFilesReader.readSchemas(setOf(resourcePath("/readme_examples/person.schema.json")))
        val mermaid = MermaidGenerator.generate(schemas)
        mermaid shouldContain "class Person"
        mermaid shouldContain "+Integer id" // required
        mermaid shouldContain "+String name" // required
        mermaid shouldContain "String email [0..1]" // optional annotated
        mermaid shouldContain "Boolean isActive [0..1]" // optional annotated
        mermaid shouldNotContain "+String email"
        mermaid shouldNotContain "+Boolean isActive"
    }

    test("Person YAML example: optional array annotated with [0..1]") {
        val schemas = SchemaFilesReader.readSchemas(setOf(resourcePath("/readme_examples/person.schema.yaml")))
        val mermaid = MermaidGenerator.generate(schemas)
        mermaid shouldContain "class Person"
        mermaid shouldContain "+Integer id"
        mermaid shouldContain "+String name"
        mermaid shouldContain "String[] tags [0..1]" // optional array cardinality
        mermaid shouldNotContain "+String[] tags"
    }

    test("Order example: required vs optional markers") {
        val schemas = SchemaFilesReader.readSchemas(setOf(resourcePath("/readme_examples/order.schema.json")))
        val mermaid = MermaidGenerator.generate(schemas)
        mermaid shouldContain "class Order"
        mermaid shouldContain "+String orderId"
        mermaid shouldContain "class Customer"
        mermaid shouldContain "+String customerId"
        mermaid shouldContain "String name [0..1]" // optional
        mermaid shouldContain "class OrderItem"
        mermaid shouldContain "+String productId"
        mermaid shouldContain "Integer quantity [0..1]" // optional
        mermaid shouldContain "Order \"1\" --> \"*\" OrderItem : items"
        mermaid shouldContain "Order \"1\" --> \"1\" Customer : customer"
        mermaid shouldNotContain "+String name"
        mermaid shouldNotContain "+Integer quantity"
    }

    test("ProductCatalog example retains required '+' without optional cardinality on required fields") {
        val schemas = SchemaFilesReader.readSchemas(setOf(resourcePath("/readme_examples/product-catalog.schema.yaml")))
        val mermaid = MermaidGenerator.generate(schemas)
        mermaid shouldContain "class ProductCatalog" // no properties
        mermaid shouldContain "class Product"
        mermaid shouldContain "+String id"
        mermaid shouldContain "+String name"
        mermaid shouldContain "+Money price"
        mermaid shouldContain "class Money"
        mermaid shouldContain "+String currency"
        mermaid shouldContain "+Number amount"
        // Ensure required fields not suffixed with [0..1]
        mermaid shouldNotContain "+String id [0..1]"
        mermaid shouldNotContain "+Money price [0..1]"
        mermaid shouldContain "ProductCatalog \"1\" --> \"*\" Product : products"
        mermaid shouldContain "Product o-- Money : price"
    }

    test("Complex example: all optional fields annotated with [0..1]") {
        val schemas = SchemaFilesReader.readSchemas(setOf(resourcePath("/readme_examples/complex.schema.json")))
        val mermaid = MermaidGenerator.generate(schemas)
        mermaid shouldContain "class ComplexExample"
        mermaid shouldContain "String id [0..1]"
        mermaid shouldContain "Map<String,String> metadata [0..1]"
        mermaid shouldContain "Map<String,Number> attributes [0..1]"
        mermaid shouldContain "class Address"
        mermaid shouldContain "String street [0..1]"
        mermaid shouldContain "String city [0..1]"
        mermaid shouldContain "ComplexExample \"1\" --> \"1\" Address : shipment"
        mermaid shouldContain "class Card"
        mermaid shouldContain "String cardNumber [0..1]"
        mermaid shouldContain "class Paypal"
        mermaid shouldContain "String eta [0..1]"
    }

    test("Extends/inheritance: optional fields show [0..1]") {
        val schemas = SchemaFilesReader.readSchemas(setOf(
            resourcePath("/readme_examples/child.schema.yaml"),
            resourcePath("/readme_examples/parent.schema.yaml")
        ))
        val mermaid = MermaidGenerator.generate(schemas)
        mermaid shouldContain "class Child"
        mermaid shouldContain "class Parent"
        mermaid shouldContain "Parent <|-- Child"
        mermaid shouldContain "Integer childField [0..1]"
        mermaid shouldContain "String parentField [0..1]"
        mermaid shouldNotContain "+Integer childField"
        mermaid shouldNotContain "+String parentField"
        val parentFieldCount = mermaid.lineSequence().count { it.contains("String parentField [0..1]") }
        parentFieldCount shouldBe 1
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
        mermaid shouldContain "Map<String,Number> attributes"
        // Should not render Map<String,Boolean> (second pattern is ignored)
        mermaid shouldNotContain "Map<String,Boolean>"
    }
})
