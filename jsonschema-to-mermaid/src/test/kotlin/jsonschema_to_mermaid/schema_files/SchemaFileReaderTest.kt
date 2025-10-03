package jsonschema_to_mermaid.schema_files

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import jsonschema_to_mermaid.exception.FileFormatException
import jsonschema_to_mermaid.schema_files.SchemaFilesReader.readSchemas
import test_util.resourcePath
import java.io.FileNotFoundException

class SchemaFileReaderTest : FunSpec({

    test("readSchemas should read a valid JSON schema file") {
        readSchemas(
            setOf(resourcePath("/bookstore/bookstore.schema.json"))
        )
            .isNotEmpty()
    }

    test("readSchemas should read a valid YAML schema file") {
        readSchemas(
            setOf(resourcePath("/bookstore/bookstore.schema.yaml"))
        )
            .isNotEmpty()
    }

    test("readSchemas should read multiple valid schema files") {
        readSchemas(
            setOf(
                resourcePath("/bookstore/bookstore.schema.json"),
                resourcePath("/bookstore/bookstore.schema.yaml")
            )
        ) shouldHaveSize 2
    }

    test("readSchemas throws an exception when reading a schema file that does not exist") {
        shouldThrow<FileNotFoundException> {
            readSchemas(setOf(resourcePath("does-not-exist")))
        }
    }

    test("readSchemas throws an exception when reading an invalid JSON schema file") {
        shouldThrow<FileFormatException> {
            readSchemas(setOf(resourcePath("/invalid/invalid.schema.json")))
        }
    }

    test("readSchemas throws an exception when reading an invalid YAML schema file") {
        shouldThrow<FileFormatException> {
            readSchemas(setOf(resourcePath("/invalid/invalid.schema.yaml")))
        }
    }
})
