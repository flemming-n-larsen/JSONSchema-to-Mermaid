package jsonschema_to_mermaid.cli

import com.google.gson.JsonObject
import io.kotest.core.spec.style.FunSpec
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe
import jsonschema_to_mermaid.diagram.AllOfMode

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

    test("CLI allof-mode=merge results in allOfMode = MERGE") {
        val options = CliOptions(allOfModeOption = "merge")
        val builder = PreferencesBuilder(options)
        val prefs = builder.build()
        prefs.allOfMode shouldBe AllOfMode.MERGE
    }

    test("CLI allof-mode=inherit results in allOfMode = INHERIT") {
        val options = CliOptions(allOfModeOption = "inherit")
        val builder = PreferencesBuilder(options)
        val prefs = builder.build()
        prefs.allOfMode shouldBe AllOfMode.INHERIT
    }

    test("CLI allof-mode=compose results in allOfMode = COMPOSE") {
        val options = CliOptions(allOfModeOption = "compose")
        val builder = PreferencesBuilder(options)
        val prefs = builder.build()
        prefs.allOfMode shouldBe AllOfMode.COMPOSE
    }

    test("Invalid CLI allof-mode value throws InvalidOptionException") {
        val options = CliOptions(allOfModeOption = "badvalue")
        val builder = PreferencesBuilder(options)
        shouldThrow<InvalidOptionException> {
            builder.build()
        }
    }

    test("Config allOfMode=inherit used when CLI absent") {
        val options = CliOptions()
        val config = JsonObject().apply { addProperty("allOfMode", "inherit") }
        val builder = PreferencesBuilder(options)
        val prefs = builder.build(config)
        prefs.allOfMode shouldBe AllOfMode.INHERIT
    }

    test("Config invalid allOfMode value throws InvalidOptionException") {
        val options = CliOptions()
        val config = JsonObject().apply { addProperty("allOfMode", "bad") }
        val builder = PreferencesBuilder(options)
        shouldThrow<InvalidOptionException> {
            builder.build(config)
        }
    }

    test("Default allOfMode is MERGE") {
        val options = CliOptions()
        val builder = PreferencesBuilder(options)
        val prefs = builder.build()
        prefs.allOfMode shouldBe AllOfMode.MERGE
    }
})

