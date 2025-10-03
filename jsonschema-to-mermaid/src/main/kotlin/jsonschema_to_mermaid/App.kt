package jsonschema_to_mermaid

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.types.path
import jsonschema_to_mermaid.schema_files.SchemaFilesReader
import java.nio.file.Path

fun main(args: Array<String>) = App().main(args)

class App : CliktCommand() {
    // Accept a single source path (file or directory) to avoid the issue where a path was tokenized into segments.
    private val source: Path by argument().path(mustExist = true)
    // Make dest optional: if not supplied, print only to stdout. If supplied, write to file and also print.
    private val dest: Path? by argument().path().optional()

    override fun run() {
        // Build the actual set of sources to scan.
        val sources = mutableSetOf<Path>()
        sources.add(source)

        var actualDest: Path? = dest
        // If dest was supplied and it's a directory, treat it as an additional source instead of a destination.
        if (dest != null && dest!!.toFile().isDirectory()) {
            sources.add(dest!!)
            actualDest = null
        }

        // Diagnostic: print resolved source paths with details
        echo("Resolved source paths:")
        sources.forEach { p ->
            val abs = try { p.toAbsolutePath().toString() } catch (_: Exception) { "<invalid>" }
            echo(" - '$p' -> abs='$abs', exists=${p.toFile().exists()}, isDirectory=${p.toFile().isDirectory()}")
        }

        val schemas = try {
            SchemaFilesReader.readSchemas(sources)
        } catch (e: Exception) {
            echo("Failed to read schemas: ${e.message}")
            throw e
        }

        // Safely handle nullable filenames when printing diagnostics
        echo("Read ${schemas.size} schema(s): ${schemas.joinToString(",") { it.filename ?: "<unknown>" }}")

        val output = try {
            MermaidGenerator.generate(schemas)
        } catch (e: Exception) {
            echo("Failed to generate Mermaid: ${e.message}")
            e.printStackTrace()
            throw e
        }

        // If a destination file was provided, write output to it.
        if (actualDest != null) {
            try {
                actualDest.toFile().writeText(output)
            } catch (e: Exception) {
                echo("Failed to write output to $actualDest: ${e.message}")
                throw e
            }
        }
        // Always print to stdout for convenience.
        print(output)
    }
}
