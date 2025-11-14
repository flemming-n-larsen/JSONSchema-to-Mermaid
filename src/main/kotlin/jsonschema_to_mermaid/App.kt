package jsonschema_to_mermaid

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.versionOption
import com.github.ajalt.clikt.parameters.types.path
import com.github.ajalt.clikt.parameters.options.flag
import jsonschema_to_mermaid.schema_files.SchemaFileInfo
import jsonschema_to_mermaid.schema_files.SchemaFilesReader
import java.nio.file.Path
import java.util.Properties

fun main(args: Array<String>) = App().main(args)

private fun loadAppNameFromProperties(): String {
    return try {
        val props = Properties()
        val stream = App::class.java.classLoader.getResourceAsStream("app.properties")
        stream?.use { props.load(it) }
        props.getProperty("appName") ?: "jsonschema-to-mermaid"
    } catch (_: Exception) {
        "jsonschema-to-mermaid"
    }
}

class App : CliktCommand(name = loadAppNameFromProperties()) {
    private val sourceFileOption: String? by option(
        "-s",
        "--source",
        help = "Name of the input JSON Schema file (relative to --source-dir or CWD)"
    )
    private val sourceDirOption: Path? by option(
        "-d",
        "--source-dir",
        help = "Path to the directory containing input JSON Schema files (default: current directory)"
    ).path(mustExist = true)
    private val sourcePath: Path? by argument(
        "source",
        help = "Path to the input JSON Schema file or directory (positional, optional; use for convenience or backward compatibility)"
    ).path(mustExist = true).optional()
    private val outputPathArg: Path? by argument("output", help = "Optional output file path").path().optional()
    private val outputPath: Path? by option("-o", "--output", help = "Write output to FILE instead of stdout").path()
    private val noClassDiagramHeader: Boolean by option(
        "--no-classdiagram-header",
        help = "Suppress the 'classDiagram' header in Mermaid output"
    ).flag(default = false)
    private val enumStyleOption: String? by option(
        "--enum-style",
        help = "Enum rendering style: inline, note, or class (default: inline)"
    )

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
        // Precedence:
        // 1. If --source-dir or --source is set, use those.
        // 2. Else, if positional <source> is set, use it as file or directory.
        // 3. Else, use CWD and all schema files.
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
            echo("Error: No schema files found to process.", err = true)
            return
        }
        val actualOutputPath = outputPath ?: outputPathArg
        echo("Using source directory: $dir", err = true)
        if (fileName != null) echo("Using source file: $fileName", err = true)
        if (positionalUsed) echo("Used positional <source> argument.", err = true)
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
        echo("Resolved source paths:", err = true)
        sources.forEach { p ->
            val abs = try {
                p.toAbsolutePath().toString()
            } catch (_: Exception) {
                "<invalid>"
            }
            echo(
                " - '$p' -> abs='$abs', exists=${p.toFile().exists()}, isDirectory=${p.toFile().isDirectory}",
                err = true
            )
        }
    }

    private fun readSchemasOrExit(sources: Set<Path>): List<SchemaFileInfo> {
        return try {
            SchemaFilesReader.readSchemas(sources)
        } catch (e: Exception) {
            echo("Failed to read schemas: ${e.message}", err = true)
            throw e
        }
    }

    private fun printSchemaDiagnostics(schemas: List<SchemaFileInfo>) {
        echo("Read ${schemas.size} schema(s): ${schemas.joinToString(",") { it.filename ?: "<unknown>" }}", err = true)
    }

    private fun generateMermaidOrExit(
        schemas: List<SchemaFileInfo>, preferences: Preferences
    ): String {
        return try {
            MermaidGenerator.generate(schemas, noClassDiagramHeader = noClassDiagramHeader, preferences = preferences)
        } catch (e: Exception) {
            echo("Failed to generate Mermaid: [31m${e.message}[0m", err = true)
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
