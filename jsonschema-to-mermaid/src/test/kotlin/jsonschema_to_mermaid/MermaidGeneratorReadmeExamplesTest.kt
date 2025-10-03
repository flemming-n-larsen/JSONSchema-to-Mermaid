package jsonschema_to_mermaid

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain
import jsonschema_to_mermaid.schema_files.SchemaFilesReader
import test_util.resourcePath

class MermaidGeneratorReadmeExamplesTest : FunSpec({

    test("Person JSON example generates expected class and fields") {
        val schemas = SchemaFilesReader.readSchemas(setOf(resourcePath("/readme_examples/person.schema.json")))
        val mermaid = MermaidGenerator.generate(schemas)

        mermaid shouldContain "class Person"
        mermaid shouldContain "+Integer id"
        mermaid shouldContain "+String name"
        mermaid shouldContain "+String email"
        mermaid shouldContain "+Boolean isActive"
    }

    test("Person YAML example generates array field as String[]") {
        val schemas = SchemaFilesReader.readSchemas(setOf(resourcePath("/readme_examples/person.schema.yaml")))
        val mermaid = MermaidGenerator.generate(schemas)

        mermaid shouldContain "class Person"
        mermaid shouldContain "+Integer id"
        mermaid shouldContain "+String name"
        mermaid shouldContain "+String[] tags"
    }

    test("Order example generates nested classes and relations") {
        val schemas = SchemaFilesReader.readSchemas(setOf(resourcePath("/readme_examples/order.schema.json")))
        val mermaid = MermaidGenerator.generate(schemas)

        mermaid shouldContain "class Order"
        mermaid shouldContain "+String orderId"
        mermaid shouldContain "class Customer"
        mermaid shouldContain "+String customerId"
        mermaid shouldContain "+String name"
        mermaid shouldContain "class OrderItem"
        mermaid shouldContain "+String productId"
        mermaid shouldContain "+Integer quantity"
        mermaid shouldContain "Order \"1\" --> \"*\" OrderItem : items"
        mermaid shouldContain "Order \"1\" --> \"1\" Customer"
    }

    test("ProductCatalog example generates definitions and \$ref relations") {
        val schemas = SchemaFilesReader.readSchemas(setOf(resourcePath("/readme_examples/product-catalog.schema.yaml")))
        val mermaid = MermaidGenerator.generate(schemas)

        mermaid shouldContain "class ProductCatalog"
        mermaid shouldContain "class Product"
        mermaid shouldContain "+String id"
        mermaid shouldContain "+String name"
        mermaid shouldContain "class Money"
        mermaid shouldContain "+String currency"
        mermaid shouldContain "+Number amount"
        mermaid shouldContain "ProductCatalog \"1\" --> \"*\" Product : products"
        mermaid shouldContain "Product o-- Money : price"
    }

    test("Complex example handles maps, definitions and composition keywords") {
        val schemas = SchemaFilesReader.readSchemas(setOf(resourcePath("/readme_examples/complex.schema.json")))
        val mermaid = MermaidGenerator.generate(schemas)

        mermaid shouldContain "class ComplexExample"
        mermaid shouldContain "+String id"
        mermaid shouldContain "+Map<String,String> metadata"
        mermaid shouldContain "class Address"
        mermaid shouldContain "+String street"
        mermaid shouldContain "+String city"
        mermaid shouldContain "ComplexExample \"1\" --> \"1\" Address : shipment"
        mermaid shouldContain "class Card"
        mermaid shouldContain "class Paypal"
        mermaid shouldContain "paymentMethod"
    }

    test("Extends/inheritance generates correct Mermaid inheritance arrow and fields") {
        val schemas = SchemaFilesReader.readSchemas(setOf(
            resourcePath("/readme_examples/child.schema.yaml"),
            resourcePath("/readme_examples/parent.schema.yaml")
        ))
        val mermaid = MermaidGenerator.generate(schemas)

        mermaid shouldContain "class Child"
        mermaid shouldContain "class Parent"
        mermaid shouldContain "Child <|-- Parent"
        mermaid shouldContain "+Integer childField"
        mermaid shouldContain "+String parentField"
    }
})
