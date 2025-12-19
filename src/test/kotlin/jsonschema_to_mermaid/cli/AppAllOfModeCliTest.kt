package jsonschema_to_mermaid.cli

import com.github.ajalt.clikt.core.main
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import test_util.resourcePath
import java.io.ByteArrayOutputStream
import java.io.PrintStream

@Suppress("unused")
class AppAllOfModeCliTest : FunSpec({
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

    test("CLI default allof-mode is merge") {
        val schemaPath = resourcePath("/core/allof_merge.schema.json").toString()
        val output = runCli(schemaPath)
        output.shouldContain("class Product")
        output.shouldContain("String name")
        output.shouldContain("Number price")
        output.shouldContain("Number discount")
    }

    test("CLI --allof-mode merge merges all properties") {
        val schemaPath = resourcePath("/core/allof_merge.schema.json").toString()
        val output = runCli(schemaPath, "--allof-mode", "merge")
        output.shouldContain("class Product")
        output.shouldContain("String name")
        output.shouldContain("Number price")
        output.shouldContain("Number discount")
        output.shouldNotContain("<|--")
        output.shouldNotContain("*--")
    }

    test("CLI --allof-mode inherit creates inheritance relationships") {
        val schemaPath = resourcePath("/core/allof_inherit.schema.json").toString()
        val output = runCli(schemaPath, "--allof-mode", "inherit")
        output.shouldContain("Person <|-- Employee")
        output.shouldContain("Worker <|-- Employee")
        output.shouldContain("class Employee")
        output.shouldContain("class Person")
        output.shouldContain("class Worker")
    }

    test("CLI --allof-mode compose creates composition relationships") {
        val schemaPath = resourcePath("/core/allof_compose.schema.json").toString()
        val output = runCli(schemaPath, "--allof-mode", "compose")
        output.shouldContain("Car *-- Engine")
        output.shouldContain("Car *-- Wheels")
        output.shouldContain("class Car")
        output.shouldContain("class Engine")
        output.shouldContain("class Wheels")
    }

    test("CLI --allof-mode case insensitive") {
        val schemaPath = resourcePath("/core/allof_inherit.schema.json").toString()
        val output = runCli(schemaPath, "--allof-mode", "INHERIT")
        output.shouldContain("Person <|-- Employee")
        output.shouldContain("Worker <|-- Employee")
    }
})

