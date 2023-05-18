package jsonschema_to_mermaid

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import util.resourcePath

const val Schema_2020_12 = "https://json-schema.org/draft/2020-12/schema"

class SchemaFilesReaderTest : FunSpec({

    context("core") {

        test("Read YAML and JSON files only from /core directory and its sub directories") {

            val schemas = SchemaFilesReader.readSchemas(setOf(resourcePath("/core")))
            schemas.size shouldBe 5

            schemas.first { it.`$id` == "core.yaml" }.`$schema` shouldBe Schema_2020_12
            schemas.first { it.`$id` == "sub-dir/sub1.json" }.`$schema` shouldBe Schema_2020_12
            schemas.first { it.`$id` == "sub-dir/sub2.yaml" }.`$schema` shouldBe Schema_2020_12
            schemas.first { it.`$id` == "sub-dir/sub-sub-dir/sub-sub1.json" }.`$schema` shouldBe Schema_2020_12
            schemas.first { it.`$id` == "sub-dir/sub-sub-dir/sub-sub2.yml" }.`$schema` shouldBe Schema_2020_12
        }
    }
})