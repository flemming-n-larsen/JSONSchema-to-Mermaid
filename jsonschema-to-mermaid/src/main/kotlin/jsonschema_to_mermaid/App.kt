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
    private val source: Path by argument().path(mustExist = true)
    private val dest: Path? by argument().path().optional()

    override fun run() {
        val sources = resolveSources(source, dest)
        val actualDest = resolveDestination(dest)
        printDiagnostics(sources)
        val schemas = readSchemasOrExit(sources)
        printSchemaDiagnostics(schemas)
        val output = generateMermaidOrExit(schemas)
        writeOutputIfNeeded(actualDest, output)
        print(output)
    }

    private fun resolveSources(source: Path, dest: Path?): MutableSet<Path> {
        val sources = mutableSetOf<Path>()
        sources.add(source)
        if (dest != null && dest.toFile().isDirectory) {
            sources.add(dest)
        }
        return sources
    }

    private fun resolveDestination(dest: Path?): Path? {
        return if (dest != null && dest.toFile().isDirectory) null else dest
    }

    private fun printDiagnostics(sources: Set<Path>) {
        echo("Resolved source paths:", err = true)
        sources.forEach { p ->
            val abs = try { p.toAbsolutePath().toString() } catch (_: Exception) { "<invalid>" }
            echo(" - '$p' -> abs='$abs', exists=${p.toFile().exists()}, isDirectory=${p.toFile().isDirectory}", err = true)
        }
    }

    private fun readSchemasOrExit(sources: Set<Path>): List<jsonschema_to_mermaid.schema_files.SchemaFileInfo> {
        return try {
            SchemaFilesReader.readSchemas(sources)
        } catch (e: Exception) {
            echo("Failed to read schemas: ${e.message}", err = true)
            throw e
        }
    }

    private fun printSchemaDiagnostics(schemas: List<jsonschema_to_mermaid.schema_files.SchemaFileInfo>) {
        echo("Read ${schemas.size} schema(s): ${schemas.joinToString(",") { it.filename ?: "<unknown>" }}", err = true)
    }

    private fun generateMermaidOrExit(schemas: List<jsonschema_to_mermaid.schema_files.SchemaFileInfo>): String {
        return try {
            MermaidGenerator.generate(schemas)
        } catch (e: Exception) {
            echo("Failed to generate Mermaid: ${e.message}", err = true)
            e.printStackTrace()
            throw e
        }
    }

    private fun writeOutputIfNeeded(dest: Path?, output: String) {
        if (dest != null) {
            try {
                dest.toFile().writeText(output)
            } catch (e: Exception) {
                echo("Failed to write output to $dest: ${e.message}", err = true)
                throw e
            }
        }
    }
}
