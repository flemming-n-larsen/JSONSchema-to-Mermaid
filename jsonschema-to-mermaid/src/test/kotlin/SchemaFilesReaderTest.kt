import com.github.flemming_n_larsen.mermaid_class_diagram_generator.SchemaFilesReader
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import util.resourcePath

const val Schema_2020_12 = "https://json-schema.org/draft/2020-12/schema"

class SchemaFilesReaderTest : FunSpec({

    context("core") {

        test("Read YAML and JSON files only from /core directory and its sub directories") {

            val schemas = SchemaFilesReader.readSchemas(setOf(resourcePath("/core")))
            schemas.size shouldBe 5

            schemas.first { it.dollarId == "core.yaml" }.dollarSchema shouldBe Schema_2020_12
            schemas.first { it.dollarId == "sub-dir/sub1.json" }.dollarSchema shouldBe Schema_2020_12
            schemas.first { it.dollarId == "sub-dir/sub2.yaml" }.dollarSchema shouldBe Schema_2020_12
            schemas.first { it.dollarId == "sub-dir/sub-sub-dir/sub-sub1.json" }.dollarSchema shouldBe Schema_2020_12
            schemas.first { it.dollarId == "sub-dir/sub-sub-dir/sub-sub2.yml" }.dollarSchema shouldBe Schema_2020_12
        }
    }
})