package com.github.flemming_n_larsen.mermaid_class_diagram_generator

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.arguments.unique
import com.github.ajalt.clikt.parameters.types.path
import java.nio.file.Path

fun main(args: Array<String>) = GenerateClassDiagrams().main(args)

class GenerateClassDiagrams : CliktCommand() {
    private val source: Set<Path> by argument().path(mustExist = true).multiple().unique()
    private val dest: Path by argument().path()

    private val jsonFileExtRegex = Regex("(?i).json$")
    private val yamlFileExtRegex = Regex("(?i).yaml$")
    private val ymlFileExtRegex = Regex("(?i).yml$")

    override fun run() {
        val strBuilder = StringBuilder()
        outputSchemas(SchemaFilesReader.readSchemas(source), strBuilder)

        print(strBuilder)
    }

    private fun outputSchemas(schemas: Collection<Schema>, strBuilder: StringBuilder) {
        strBuilder.append("classDiagram\n")
        schemas.forEach { outputSchema(it, strBuilder) }
    }

    private fun outputSchema(schema: Schema, strBuilder: StringBuilder) {
        strBuilder.append("class ").append(toClassName(schema.dollarId!!))
        if (schema.properties?.isNotEmpty() == true) {
            strBuilder.append("{")
            strBuilder.append("}")
        } else {
            strBuilder.append("\n")
        }
    }

    private fun toClassName(schemaId: String): String {
        var className = schemaId.trim()
        val lastIndex = schemaId.lastIndexOf("/")
        if (lastIndex >= 0) {
            className = schemaId.substring(lastIndex + 1)
        }
        className = className.replace(jsonFileExtRegex, "")
        className = className.replace(yamlFileExtRegex, "")
        className = className.replace(ymlFileExtRegex, "")
        return className
    }
}
