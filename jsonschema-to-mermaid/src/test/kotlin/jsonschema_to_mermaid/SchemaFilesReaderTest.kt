package jsonschema_to_mermaid

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldEndWith
import util.resourcePath

const val Schema_2020_12 = "https://json-schema.org/draft/2020-12/schema"

class SchemaFilesReaderTest : FunSpec({

    context("core") {

        test("Read YAML and JSON files only from /core directory and its sub directories") {

            val schemas = SchemaFilesReader.readSchemas(setOf(resourcePath("/core")))
            schemas.size shouldBe 5

            schemas.first { it.schema.`$id` == "core.yaml" }.apply {
                filename?.shouldEndWith("core.yaml")
                schema.`$schema` shouldBe Schema_2020_12
            }
            schemas.first { it.schema.`$id` == "sub-dir/sub1.json" }.apply {
                filename?.shouldEndWith("sub1.json")
                schema.`$schema` shouldBe Schema_2020_12
            }
            schemas.first { it.schema.`$id` == "sub-dir/sub2.yaml" }.apply {
                filename?.shouldEndWith("sub2.yaml")
                schema.`$schema` shouldBe Schema_2020_12
            }
            schemas.first { it.schema.`$id` == "sub-dir/sub-sub-dir/sub-sub1.json" }.apply {
                filename?.shouldEndWith("sub-sub1.json")
                schema.`$schema` shouldBe Schema_2020_12
            }
            schemas.first { it.schema.`$id` == "sub-dir/sub-sub-dir/sub-sub2.yml" }.apply {
                filename?.shouldEndWith("sub-sub2.yml")
                schema.`$schema` shouldBe Schema_2020_12
            }
        }
    }
})