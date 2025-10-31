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
    private val sourcePath: Path by argument().path(mustExist = true)
    private val destinationPath: Path? by argument().path().optional()

    override fun run() {
        val sources = resolveSources(sourcePath, destinationPath)
        val actualDestinationPath = resolveDestination(destinationPath)
        printDiagnostics(sources)
        val schemas = readSchemasOrExit(sources)
        printSchemaDiagnostics(schemas)
        val output = generateMermaidOrExit(schemas)
        writeOutputIfNeeded(actualDestinationPath, output)
        print(output)
    }

    private fun resolveSources(sourcePath: Path, destinationPath: Path?): MutableSet<Path> {
        val sources = mutableSetOf<Path>()
        sources.add(sourcePath)
        if (destinationPath != null && destinationPath.toFile().isDirectory) {
            sources.add(destinationPath)
        }
        return sources
    }

    private fun resolveDestination(destinationPath: Path?): Path? {
        return if (destinationPath != null && destinationPath.toFile().isDirectory) null else destinationPath
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

    private fun writeOutputIfNeeded(outputPath: Path?, output: String) {
        if (outputPath != null) {
            try {
                outputPath.toFile().writeText(output)
            } catch (e: Exception) {
                echo("Failed to write output to $outputPath: ${e.message}", err = true)
                throw e
            }
        }
    }
}
