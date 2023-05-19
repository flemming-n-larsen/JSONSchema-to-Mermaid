package jsonschema_to_mermaid

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

class MermaidGeneratorTest  : FunSpec({

    context("title") {

        test("Test that title is found and when filepath, \$id, and title exists") {
            val schema = Schema()
            schema.`$id` = "something"
            schema.title = "  Foo Bar "
            val schemaData = SchemaData("foo-bar.json", schema)

            val output = MermaidGenerator.generate(listOf(schemaData))
            output.indexOf("class Foo Bar") shouldBeGreaterThan 0
        }

        test("Test that file name without extension is found and used when title is missing, but filepath and \$id exists") {
            val schema = Schema()
            schema.`$id` = "something"
            val schemaData = SchemaData("foo-bar.json", schema)

            val output = MermaidGenerator.generate(listOf(schemaData))
            output.indexOf("class foo-bar") shouldBeGreaterThan 0
        }

        test("Test that \$id is found and used if filepath and title is missing, but \$id exists") {
            val schema = Schema()
            schema.`$id` = "  https://jsonschema-to-mermaid.io/schemas/foo-bar "
            val schemaData = SchemaData(schema = schema)

            val output = MermaidGenerator.generate(listOf(schemaData))
            output.indexOf("class foo-bar") shouldBeGreaterThan 0
        }

        test("Test that error occur if filepath, title, and \$id is missing") {
            val schema = SchemaData(schema = Schema())

            shouldThrow<IllegalStateException> {
                MermaidGenerator.generate(listOf(schema))
            }.apply {
                message shouldBe "schema is missing title and \$id fields"
            }
        }
    }
})