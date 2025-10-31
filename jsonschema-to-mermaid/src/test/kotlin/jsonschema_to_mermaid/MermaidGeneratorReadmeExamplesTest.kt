package jsonschema_to_mermaid

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.shouldBe
import jsonschema_to_mermaid.schema_files.SchemaFilesReader
import test_util.resourcePath

class MermaidGeneratorReadmeExamplesTest : FunSpec({

    test("Person JSON example generates expected class and fields with correct required markers") {
        val schemas = SchemaFilesReader.readSchemas(setOf(resourcePath("/readme_examples/person.schema.json")))
        val mermaid = MermaidGenerator.generate(schemas)

        mermaid shouldContain "class Person"
        mermaid shouldContain "+Integer id" // required
        mermaid shouldContain "+String name" // required
        mermaid shouldContain "String email" // optional (no +)
        mermaid shouldContain "Boolean isActive" // optional (no +)
        mermaid shouldNotContain "+String email"
        mermaid shouldNotContain "+Boolean isActive"
    }

    test("Person YAML example generates array field without plus (optional)") {
        val schemas = SchemaFilesReader.readSchemas(setOf(resourcePath("/readme_examples/person.schema.yaml")))
        val mermaid = MermaidGenerator.generate(schemas)

        mermaid shouldContain "class Person"
        mermaid shouldContain "+Integer id"
        mermaid shouldContain "+String name"
        mermaid shouldContain "String[] tags" // optional
        mermaid shouldNotContain "+String[] tags"
    }

    test("Order example generates nested classes and relations with required markers only where appropriate") {
        val schemas = SchemaFilesReader.readSchemas(setOf(resourcePath("/readme_examples/order.schema.json")))
        val mermaid = MermaidGenerator.generate(schemas)

        mermaid shouldContain "class Order"
        mermaid shouldContain "+String orderId" // required top-level
        mermaid shouldContain "class Customer"
        mermaid shouldContain "+String customerId" // required inside customer
        mermaid shouldContain "String name" // optional inside customer
        mermaid shouldContain "class OrderItem"
        mermaid shouldContain "+String productId" // required inside item
        mermaid shouldContain "Integer quantity" // optional inside item
        mermaid shouldContain "Order \"1\" --> \"*\" OrderItem : items"
        mermaid shouldContain "Order \"1\" --> \"1\" Customer : customer"
        mermaid shouldNotContain "+String name"
        mermaid shouldNotContain "+Integer quantity"
    }

    test("ProductCatalog example generates definitions and \$ref relations with required markers") {
        val schemas = SchemaFilesReader.readSchemas(setOf(resourcePath("/readme_examples/product-catalog.schema.yaml")))
        val mermaid = MermaidGenerator.generate(schemas)

        mermaid shouldContain "class ProductCatalog"
        mermaid shouldContain "class Product"
        mermaid shouldContain "+String id"
        mermaid shouldContain "+String name"
        mermaid shouldContain "+Money price"
        mermaid shouldContain "class Money"
        mermaid shouldContain "+String currency"
        mermaid shouldContain "+Number amount"
        mermaid shouldContain "ProductCatalog \"1\" --> \"*\" Product : products"
        mermaid shouldContain "Product o-- Money : price"
    }

    test("Complex example has no required markers (all optional)") {
        val schemas = SchemaFilesReader.readSchemas(setOf(resourcePath("/readme_examples/complex.schema.json")))
        val mermaid = MermaidGenerator.generate(schemas)

        mermaid shouldContain "class ComplexExample"
        mermaid shouldContain "String id" // optional
        mermaid shouldContain "Map<String,String> metadata" // optional
        mermaid shouldContain "class Address"
        mermaid shouldContain "String street"
        mermaid shouldContain "String city"
        mermaid shouldContain "ComplexExample \"1\" --> \"1\" Address : shipment"
        mermaid shouldContain "class Card"
        mermaid shouldContain "class Paypal"
        mermaid shouldContain "paymentMethod"
        mermaid shouldNotContain "+String id"
    }

    test("Extends/inheritance: optional fields (no plus) and inheritance arrow") {
        val schemas = SchemaFilesReader.readSchemas(setOf(
            resourcePath("/readme_examples/child.schema.yaml"),
            resourcePath("/readme_examples/parent.schema.yaml")
        ))
        val mermaid = MermaidGenerator.generate(schemas)

        mermaid shouldContain "class Child"
        mermaid shouldContain "class Parent"
        mermaid shouldContain "Parent <|-- Child"
        mermaid shouldContain "Integer childField" // optional
        mermaid shouldContain "String parentField" // optional only in Parent
        mermaid shouldNotContain "+Integer childField"
        mermaid shouldNotContain "+String parentField"
        val parentFieldCount = mermaid.lineSequence().count { it.contains("String parentField") }
        parentFieldCount shouldBe 1
    }

    test("Transitive inheritance hides inherited properties at each level (Parent <- Child <- Grandchild)") {
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
        mermaid shouldContain "String parentField"
        mermaid shouldContain "Integer childField"
        mermaid shouldContain "Boolean grandchildField"
        mermaid shouldNotContain "+String parentField"
        mermaid shouldNotContain "+Integer childField"
        mermaid shouldNotContain "+Boolean grandchildField"

        val parentFieldCount = mermaid.lineSequence().count { it.contains("String parentField") }
        val childFieldCount = mermaid.lineSequence().count { it.contains("Integer childField") }
        val grandchildFieldCount = mermaid.lineSequence().count { it.contains("Boolean grandchildField") }

        parentFieldCount shouldBe 1
        childFieldCount shouldBe 1
        grandchildFieldCount shouldBe 1
    }
})
