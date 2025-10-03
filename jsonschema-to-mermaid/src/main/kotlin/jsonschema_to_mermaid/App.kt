package jsonschema_to_mermaid

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.arguments.unique
import com.github.ajalt.clikt.parameters.types.path
import jsonschema_to_mermaid.schema_files.SchemaFilesReader
import java.nio.file.Path

fun main(args: Array<String>) = App().main(args)

class App : CliktCommand() {
    private val source: Set<Path> by argument().path(mustExist = true).multiple().unique()
    private val dest: Path by argument().path()

    override fun run() {
        val output = MermaidGenerator.generate(SchemaFilesReader.readSchemas(source))
        // Write output to the destination file so the CLI example that passes a dest path works as expected.
        try {
            dest.toFile().writeText(output)
        } catch (e: Exception) {
            echo("Failed to write output to $dest: ${'$'}{e.message}")
            throw e
        }
        // Also print to stdout for convenience (keeps previous behavior compatible with redirection).
        print(output)
    }
}
