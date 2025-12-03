package jsonschema_to_mermaid.cli

import com.github.ajalt.clikt.core.main
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import test_util.resourcePath
import java.io.ByteArrayOutputStream
import java.io.PrintStream

@Suppress("unused")
class AppEnumStyleCliTest : FunSpec({
    // Helper to run the CLI and capture stdout (Mermaid output); stderr diagnostics are discarded
    fun runCli(vararg args: String): String {
        val originalOut = System.out
        val originalErr = System.err
        val baosOut = ByteArrayOutputStream()
        val baosErr = ByteArrayOutputStream()
        System.setOut(PrintStream(baosOut))
        System.setErr(PrintStream(baosErr))
        try {
            // Invoke Clikt command directly with proper Array<String>
            App().main(arrayOf(*args))
        } finally {
            System.setOut(originalOut)
            System.setErr(originalErr)
        }
        return baosOut.toString("UTF-8")
    }

    test("CLI default enum style is inline") {
        val schemaPath = resourcePath("/readme_examples/enum-example.schema.json").toString()
        val output = runCli(schemaPath)
        output.shouldContain("{A|B|C} status")
        output.shouldNotContain("note for EnumExample")
        output.shouldNotContain("class StatusEnum")
    }

    test("CLI --enum-style note renders note style") {
        val schemaPath = resourcePath("/readme_examples/enum-example.schema.json").toString()
        val output = runCli(schemaPath, "--enum-style", "note")
        output.shouldContain("note for EnumExample \"status: A, B, C\"")
        output.shouldNotContain("{A|B|C} status")
        output.shouldNotContain("class StatusEnum")
    }

    test("CLI --enum-style class renders enum as separate class") {
        val schemaPath = resourcePath("/readme_examples/enum-example.schema.json").toString()
        val output = runCli(schemaPath, "--enum-style", "class")
        output.shouldContain("class EnumExampleStatusEnum")
        output.shouldContain("EnumExampleStatusEnum status")
        output.shouldNotContain("{A|B|C} status")
        output.shouldNotContain("note for EnumExample")
    }

    test("CLI --show-inherited-fields displays inherited properties") {
        val schemaPath = resourcePath("/readme_examples/child.schema.yaml").toString()
        val output = runCli(schemaPath, "--show-inherited-fields")
        output.shouldContain("String parentField [0..1]")
        val defaultOutput = runCli(schemaPath)
        defaultOutput.shouldNotContain("String parentField [0..1]")
    }

    test("CLI --arrays-inline renders arrays as inline fields") {
        val schemaPath = resourcePath("/readme_examples/order.schema.json").toString()
        val output = runCli(schemaPath, "--arrays-inline")
        output.shouldContain("Object[] Item")
        output.shouldNotContain("Order \"1\" --> \"*\" OrderItem : items")
    }

    test("CLI default renders arrays as relations") {
        val schemaPath = resourcePath("/readme_examples/order.schema.json").toString()
        val output = runCli(schemaPath)
        output.shouldContain("Order \"1\" --> \"*\" OrderItem : items")
    }

    test("CLI --required-style none removes markers") {
        val schemaPath = resourcePath("/readme_examples/person.schema.json").toString()
        val output = runCli(schemaPath, "--required-style", "none")
        output.shouldNotContain("+")
        output.shouldNotContain("[0..1]")
        output.shouldContain("Integer id")
    }

    test("CLI --required-style suffix-q appends question mark to optional") {
        val schemaPath = resourcePath("/readme_examples/person.schema.json").toString()
        val output = runCli(schemaPath, "--required-style", "suffix-q")
        output.shouldContain("String email?")
        output.shouldNotContain("[0..1]")
    }
})
