package jsonschema_to_mermaid.cli

import jsonschema_to_mermaid.schema_files.SchemaFileInfo
import jsonschema_to_mermaid.schema_files.SchemaFilesReader
import jsonschema_to_mermaid.diagram.Preferences
import jsonschema_to_mermaid.diagram.EnumStyle
import jsonschema_to_mermaid.diagram.MermaidGenerator
import java.nio.file.Path

/**
 * Data class for CLI options, making it easy to extend and maintain.
 */
data class CliOptions(
    val sourceFileOption: String? = null,
    val sourceDirOption: Path? = null,
    val sourcePath: Path? = null,
    val outputPathArg: Path? = null,
    val outputPath: Path? = null,
    val noClassDiagramHeader: Boolean = false,
    val enumStyleOption: String? = null
)

/**
 * Handles CLI business logic, separated from CLI parsing.
 */
class CliService(
    private val options: CliOptions,
    private val echo: (String, Boolean) -> Unit
) {
    fun execute() {
        val (dir, fileName, positionalUsed) = resolveDirectoryAndFile()
        val sources = resolveSources(dir, fileName)
        if (sources.isEmpty()) {
            echo("Error: No schema files found to process.", true)
            return
        }
        val actualOutputPath = options.outputPath ?: options.outputPathArg
        printSourceDiagnostics(dir, fileName, positionalUsed)
        printDiagnostics(sources)
        val schemas = readSchemasOrExit(sources)
        printSchemaDiagnostics(schemas)
        val output = generateMermaidOrExit(schemas, buildPreferences())
        writeOutputIfNeeded(actualOutputPath, output)
        if (actualOutputPath == null) print(output)
    }

    private fun resolveDirectoryAndFile(): Triple<Path, String?, Boolean> {
        var dir: Path? = null
        var fileName: String? = null
        var positionalUsed = false
        if (options.sourceDirOption != null || options.sourceFileOption != null) {
            dir = options.sourceDirOption ?: java.nio.file.Paths.get("").toAbsolutePath()
            fileName = options.sourceFileOption
        } else if (options.sourcePath != null) {
            positionalUsed = true
            val file = options.sourcePath
            if (file.toFile().exists()) {
                if (file.toFile().isDirectory) {
                    dir = file
                    fileName = null
                } else {
                    dir = file.parent ?: java.nio.file.Paths.get("").toAbsolutePath()
                    fileName = file.fileName.toString()
                }
            }
        } else {
            dir = java.nio.file.Paths.get("").toAbsolutePath()
            fileName = null
        }
        return Triple(dir!!, fileName, positionalUsed)
    }

    private fun resolveSources(dir: Path, fileName: String?): MutableSet<Path> {
        val sources = mutableSetOf<Path>()
        if (fileName != null) {
            val file = dir.resolve(fileName)
            if (file.toFile().exists()) sources.add(file)
        } else {
            val files = dir.toFile().listFiles { f ->
                f.isFile && (f.name.endsWith(".json") || f.name.endsWith(".yaml") || f.name.endsWith(".yml"))
            } ?: emptyArray()
            sources.addAll(files.map { it.toPath() })
        }
        return sources
    }

    private fun printSourceDiagnostics(dir: Path, fileName: String?, positionalUsed: Boolean) {
        echo("Using source directory: $dir", true)
        if (fileName != null) echo("Using source file: $fileName", true)
        if (positionalUsed) echo("Used positional <source> argument.", true)
    }

    private fun printDiagnostics(sources: Set<Path>) {
        echo("Resolved source paths:", true)
        sources.forEach { p ->
            val abs = try {
                p.toAbsolutePath().toString()
            } catch (_: Exception) {
                "<invalid>"
            }
            echo(
                " - '$p' -> abs='$abs', exists=${p.toFile().exists()}, isDirectory=${p.toFile().isDirectory}",
                true
            )
        }
    }

    private fun readSchemasOrExit(sources: Set<Path>): List<SchemaFileInfo> {
        return try {
            SchemaFilesReader.readSchemas(sources)
        } catch (e: Exception) {
            echo("Failed to read schemas: ${e.message}", true)
            throw e
        }
    }

    private fun printSchemaDiagnostics(schemas: List<SchemaFileInfo>) {
        echo("Read ${schemas.size} schema(s): ${schemas.joinToString(",") { it.filename ?: "<unknown>" }}", true)
    }

    private fun generateMermaidOrExit(
        schemas: List<SchemaFileInfo>, preferences: Preferences
    ): String {
        return try {
            MermaidGenerator.generate(schemas, noClassDiagramHeader = options.noClassDiagramHeader, preferences = preferences)
        } catch (e: Exception) {
            echo("Failed to generate Mermaid: \u001B[31m${e.message}\u001B[0m", true)
            e.printStackTrace()
            throw e
        }
    }

    private fun writeOutputIfNeeded(outputPath: Path?, output: String) {
        if (outputPath != null) {
            try {
                outputPath.toFile().writeText(output)
            } catch (e: Exception) {
                echo("Failed to write output to $outputPath: ${e.message}", true)
                throw e
            }
        }
    }

    private fun buildPreferences(): Preferences {
        val enumStyle = when (options.enumStyleOption?.lowercase()) {
            "note" -> EnumStyle.NOTE
            "class" -> EnumStyle.CLASS
            else -> EnumStyle.INLINE
        }
        return Preferences(enumStyle = enumStyle)
    }
}
