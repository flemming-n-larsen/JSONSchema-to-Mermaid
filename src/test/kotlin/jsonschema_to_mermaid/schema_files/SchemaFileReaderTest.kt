package jsonschema_to_mermaid.schema_files

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import jsonschema_to_mermaid.exception.FileFormatException
import jsonschema_to_mermaid.exception.InheritanceCycleException
import test_util.resourcePath
import java.io.FileNotFoundException

class SchemaFileReaderTest : FunSpec({

    test("readSchemas should read a valid JSON schema file") {
        SchemaFilesReader.readSchemas(
            setOf(resourcePath("/bookstore/bookstore.schema.json"))
        )
            .isNotEmpty()
    }

    test("readSchemas should read a valid YAML schema file") {
        SchemaFilesReader.readSchemas(
            setOf(resourcePath("/bookstore/bookstore.schema.yaml"))
        )
            .isNotEmpty()
    }

    test("readSchemas should read multiple valid schema files") {
        SchemaFilesReader.readSchemas(
            setOf(
                resourcePath("/bookstore/bookstore.schema.json"),
                resourcePath("/bookstore/bookstore.schema.yaml")
            )
        ) shouldHaveSize 2
    }

    test("readSchemas throws an exception when reading a schema file that does not exist") {
        shouldThrow<FileNotFoundException> {
            SchemaFilesReader.readSchemas(setOf(resourcePath("does-not-exist")))
        }
    }

    test("readSchemas throws an exception when reading an invalid JSON schema file") {
        shouldThrow<FileFormatException> {
            SchemaFilesReader.readSchemas(setOf(resourcePath("/invalid/invalid.schema.json")))
        }
    }

    test("readSchemas throws an exception when reading an invalid YAML schema file") {
        shouldThrow<FileFormatException> {
            SchemaFilesReader.readSchemas(setOf(resourcePath("/invalid/invalid.schema.yaml")))
        }
    }

    test("readSchemas throws InheritanceCycleException on circular extends chain") {
        shouldThrow<InheritanceCycleException> {
            SchemaFilesReader.readSchemas(
                setOf(
                    resourcePath("/readme_examples/cycle-a.schema.yaml"),
                    resourcePath("/readme_examples/cycle-b.schema.yaml")
                )
            )
        }
    }

    test("readSchemas resolves external $ref from file") {
        val schemas = SchemaFilesReader.readSchemas(
            setOf(resourcePath("/readme_examples/external-ref-main.schema.json"))
        )
        schemas.shouldHaveSize(1)
        val main = schemas.first().schema
        // The external property should be present and resolved
        main.properties?.containsKey("externalProperty") shouldBe true
    }

    test("readSchemas resolves external $ref from HTTP URL") {
        val schemas = SchemaFilesReader.readSchemas(
            setOf(resourcePath("/readme_examples/http-ref-main.schema.json"))
        )
        schemas.shouldHaveSize(1)
        val main = schemas.first().schema
        // The property from the HTTP schema should be present
        main.properties?.containsKey("name") shouldBe true // 'name' is a property in package.json schema
    }
})
