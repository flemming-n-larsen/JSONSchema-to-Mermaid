package jsonschema_to_mermaid.cli

import com.google.gson.JsonObject
import io.kotest.core.spec.style.FunSpec
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue

class PreferencesBuilderTest : FunSpec({
    test("CLI arrays=inline results in arraysAsRelation = false") {
        val options = CliOptions(arraysOption = "inline")
        val builder = PreferencesBuilder(options)
        val prefs = builder.build()
        prefs.arraysAsRelation.shouldBeFalse()
    }

    test("CLI arrays=relation results in arraysAsRelation = true") {
        val options = CliOptions(arraysOption = "relation")
        val builder = PreferencesBuilder(options)
        val prefs = builder.build()
        prefs.arraysAsRelation.shouldBeTrue()
    }

    test("Invalid CLI arrays value throws InvalidOptionException") {
        val options = CliOptions(arraysOption = "badvalue")
        val builder = PreferencesBuilder(options)
        shouldThrow<InvalidOptionException> {
            builder.build()
        }
    }

    test("Config arrays=inline used when CLI absent") {
        val options = CliOptions()
        val config = JsonObject().apply { addProperty("arrays", "inline") }
        val builder = PreferencesBuilder(options)
        val prefs = builder.build(config)
        prefs.arraysAsRelation.shouldBeFalse()
    }

    test("Config invalid arrays value throws InvalidOptionException") {
        val options = CliOptions()
        val config = JsonObject().apply { addProperty("arrays", "bad") }
        val builder = PreferencesBuilder(options)
        shouldThrow<InvalidOptionException> {
            builder.build(config)
        }
    }
})

