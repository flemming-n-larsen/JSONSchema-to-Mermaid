package jsonschema_to_mermaid

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.arguments.unique
import com.github.ajalt.clikt.parameters.types.path
import jsonschema_to_mermaid.file.SchemaFilesReader
import java.nio.file.Path

fun main(args: Array<String>) = GenerateClassDiagrams.main(args)

object GenerateClassDiagrams : CliktCommand() {
    private val source: Set<Path> by argument().path(mustExist = true).multiple().unique()
    private val dest: Path by argument().path()

    override fun run() {
        val output = MermaidGenerator.generate(SchemaFilesReader.readSchemas(source))
        print(output)
    }
}
