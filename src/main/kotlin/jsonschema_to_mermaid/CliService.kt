package jsonschema_to_mermaid

import jsonschema_to_mermaid.schema_files.SchemaFileInfo
import jsonschema_to_mermaid.schema_files.SchemaFilesReader
import java.nio.file.Path

/**
 * Handles CLI business logic, separated from CLI parsing.
 */
class CliService(
    private val sourceFileOption: String?,
    private val sourceDirOption: Path?,
    private val sourcePath: Path?,
    private val outputPathArg: Path?,
    private val outputPath: Path?,
    private val noClassDiagramHeader: Boolean,
    private val enumStyleOption: String?,
    private val echo: (String, Boolean) -> Unit
) {
    fun execute() {
        var dir: Path? = null
        var fileName: String? = null
        var positionalUsed = false
        if (sourceDirOption != null || sourceFileOption != null) {
            dir = sourceDirOption ?: java.nio.file.Paths.get("").toAbsolutePath()
            fileName = sourceFileOption
        } else if (sourcePath != null) {
            positionalUsed = true
            val file = sourcePath
            if (file != null && file.toFile().exists()) {
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
        val sources = resolveSources(dir!!, fileName)
        if (sources.isEmpty()) {
            echo("Error: No schema files found to process.", true)
            return
        }
        val actualOutputPath = outputPath ?: outputPathArg
        echo("Using source directory: $dir", true)
        if (fileName != null) echo("Using source file: $fileName", true)
        if (positionalUsed) echo("Used positional <source> argument.", true)
        printDiagnostics(sources)
        val schemas = readSchemasOrExit(sources)
        printSchemaDiagnostics(schemas)
        val enumStyle = when (enumStyleOption?.lowercase()) {
            "note" -> EnumStyle.NOTE
            "class" -> EnumStyle.CLASS
            else -> EnumStyle.INLINE
        }
        val preferences = Preferences(enumStyle = enumStyle)
        val output = generateMermaidOrExit(schemas, preferences)
        writeOutputIfNeeded(actualOutputPath, output)
        if (actualOutputPath == null) print(output)
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
            MermaidGenerator.generate(schemas, noClassDiagramHeader = noClassDiagramHeader, preferences = preferences)
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
}

