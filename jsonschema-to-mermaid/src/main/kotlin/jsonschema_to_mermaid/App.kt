package jsonschema_to_mermaid

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.versionOption
import com.github.ajalt.clikt.parameters.types.path
import jsonschema_to_mermaid.schema_files.SchemaFilesReader
import java.nio.file.Path
import java.util.Properties

fun main(args: Array<String>) = App().main(args)

class App : CliktCommand() {
    private val sourcePath: Path by argument("source", help = "Path to the input JSON Schema file or directory").path(mustExist = true)
    private val outputPathArg: Path? by argument("output", help = "Optional output file path").path().optional()
    private val outputPath: Path? by option("-o", "--output", help = "Write output to FILE instead of stdout").path()
    // The following options are documented but not yet supported in the generator implementation:
    // private val root: String? by option("-r", "--root", help = "Use NAME as the root definition")
    // private val noHeader: Boolean by option("--no-header", help = "Suppress the Mermaid header in output").flag(default = false)
    init {
        versionOption(loadVersionFromProperties())
    }

    private fun loadVersionFromProperties(): String {
        return try {
            val props = Properties()
            val stream = this::class.java.classLoader.getResourceAsStream("version.properties")
            stream?.use { props.load(it) }
            props.getProperty("version") ?: "<unknown>"
        } catch (_: Exception) {
            "<unknown>"
        }
    }

    override fun run() {
        val actualOutputPath = outputPath ?: outputPathArg
        val sources = resolveSources(sourcePath)
        printDiagnostics(sources)
        val schemas = readSchemasOrExit(sources)
        printSchemaDiagnostics(schemas)
        val output = generateMermaidOrExit(schemas)
        writeOutputIfNeeded(actualOutputPath, output)
        if (actualOutputPath == null) print(output)
    }

    private fun resolveSources(sourcePath: Path): MutableSet<Path> {
        val sources = mutableSetOf<Path>()
        sources.add(sourcePath)
        return sources
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

    private fun generateMermaidOrExit(
        schemas: List<jsonschema_to_mermaid.schema_files.SchemaFileInfo>
    ): String {
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
