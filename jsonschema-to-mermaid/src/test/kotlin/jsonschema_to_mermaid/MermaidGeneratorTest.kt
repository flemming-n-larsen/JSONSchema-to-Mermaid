package jsonschema_to_mermaid

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

class MermaidGeneratorTest  : FunSpec({

    context("title") {

        test("Test that title is found and used") {
            val schema = Schema()
            schema.title = "  Foo Bar "

            val output = MermaidGenerator.generate(listOf(schema))
            output.indexOf("class Foo Bar") shouldBeGreaterThan 0
        }

        test("Test that \$id is found and used if title is missing") {
            val schema = Schema()
            schema.`$id` = "  https://jsonschema-to-mermaid.io/schemas/foo-bar "

            val output = MermaidGenerator.generate(listOf(schema))
            output.indexOf("class foo-bar") shouldBeGreaterThan 0
        }

        test("Test that error occur if both title and \$id is missing") {
            val schema = Schema()

            shouldThrow<IllegalStateException> {
                MermaidGenerator.generate(listOf(schema))
            }.apply {
                message shouldBe "schema is missing title and \$id fields"
            }
        }
    }
})