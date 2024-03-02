package jsonschema_to_mermaid.file

import com.google.gson.JsonSyntaxException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.throwable.shouldHaveMessage
import test_util.resourcePath
import java.io.FileNotFoundException

class SchemaFileReaderTest : FunSpec({

    test("readSchemas should read valid JSON file") {
        val schema = SchemaFilesReader.readSchemas(setOf(resourcePath("/bookstore/bookstore.schema.json")))
        schema.isNotEmpty()
    }

    test("readSchemas throws FileNotFoundException when reading a file that does not exist") {
        shouldThrow<FileNotFoundException> {
            SchemaFilesReader.readSchemas(setOf(resourcePath("does-not-exist")))
        }
    }

    test("readSchemas throws JsonSyntaxException when reading invalid JSON file") {
        val exception = shouldThrow<JsonSyntaxException> {
            SchemaFilesReader.readSchemas(setOf(resourcePath("/invalid/invalid.schema.json"))) }
        exception shouldHaveMessage """.*MalformedJsonException: Expected ':'.*""".toRegex()
    }
})