package jsonschema_to_mermaid.cli

import jsonschema_to_mermaid.schema_files.SchemaFileInfo
import jsonschema_to_mermaid.schema_files.SchemaFilesReader
import jsonschema_to_mermaid.diagram.Preferences
import jsonschema_to_mermaid.diagram.EnumStyle
import jsonschema_to_mermaid.diagram.MermaidGenerator
import jsonschema_to_mermaid.diagram.RequiredFieldStyle
import java.nio.file.Path
import java.nio.file.Files
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException

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
    val enumStyleOption: String? = null,
    val configFile: Path? = null,
    val useEnglishSingularizer: Boolean = true,
    val showInheritedFields: Boolean = false,
    val arraysAsRelation: Boolean = true,
    val arraysInline: Boolean = false,
    val requiredStyleOption: String? = null
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
        val resolvedConfig = resolveConfigFile(dir)
        val preferences = try {
            buildPreferences(resolvedConfig)
        } catch (e: IllegalArgumentException) {
            // Print the error to stderr and exit gracefully
            echo("${e.message}", true)
            System.err.println(e.message)
            return
        }
        val output = generateMermaidOrExit(schemas, preferences)
        writeOutputIfNeeded(actualOutputPath, output)
        if (actualOutputPath == null) print(output)
    }

    /**
     * Discover a config file if not provided explicitly. Precedence:
     * 1) explicit --config-file (already in options)
     * 2) project-level files in the source directory: js2m.json, .js2mrc
     * 3) user-level in HOME: .js2m.json, .js2mrc
     */
    private fun resolveConfigFile(sourceDir: Path): Path? {
        if (options.configFile != null) return options.configFile
        val candidates = listOf("js2m.json", ".js2mrc")
        for (name in candidates) {
            val p = sourceDir.resolve(name)
            if (p.toFile().exists() && p.toFile().isFile) return p
        }
        val home = System.getProperty("user.home") ?: return null
        for (name in listOf(".js2m.json", ".js2mrc")) {
            val p = java.nio.file.Paths.get(home).resolve(name)
            if (p.toFile().exists() && p.toFile().isFile) return p
        }
        return null
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
            val excludeNames = setOf("js2m.json", ".js2mrc", ".js2m.json")
            val files = dir.toFile().listFiles { f ->
                f.isFile && (f.name.endsWith(".json") || f.name.endsWith(".yaml") || f.name.endsWith(".yml")) && !excludeNames.contains(f.name)
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

    private fun buildPreferences(resolvedConfig: Path?): Preferences {
        // Load config file defaults (if present) and then override with explicit CLI flags.
        var configJson: JsonObject? = null
        if (resolvedConfig != null) {
            try {
                val text = String(Files.readAllBytes(resolvedConfig))
                configJson = Gson().fromJson(text, JsonObject::class.java)
            } catch (e: JsonSyntaxException) {
                throw IllegalArgumentException("Invalid JSON in config file: ${e.message}")
            }
        }

        fun configString(key: String): String? = configJson?.get(key)?.asString

        val enumStyle = when (options.enumStyleOption?.lowercase()) {
            "note" -> EnumStyle.NOTE
            "class" -> EnumStyle.CLASS
            null, "inline" -> EnumStyle.INLINE
            else -> throw IllegalArgumentException("Invalid enum style: ${options.enumStyleOption}")
        }
        val arraysPreference = when {
            options.arraysInline -> false
            configString("arrays") != null -> when (configString("arrays")!!.lowercase()) {
                "inline" -> false
                "relation" -> true
                else -> throw IllegalArgumentException("Invalid arrays value in config: ${configString("arrays")}")
            }
            else -> options.arraysAsRelation
        }
        val requiredStyle = when (options.requiredStyleOption?.lowercase()) {
            null, "plus" -> RequiredFieldStyle.PLUS
            "none" -> RequiredFieldStyle.NONE
            "suffix-q" -> RequiredFieldStyle.SUFFIX_Q
            else -> throw IllegalArgumentException("Invalid required style: ${options.requiredStyleOption}")
        }
        // If not provided on CLI, allow config file to set requiredStyle
        val requiredStyleFinal = if (options.requiredStyleOption == null && configString("requiredStyle") != null) {
            when (configString("requiredStyle")!!.lowercase()) {
                "plus" -> RequiredFieldStyle.PLUS
                "none" -> RequiredFieldStyle.NONE
                "suffix-q" -> RequiredFieldStyle.SUFFIX_Q
                else -> throw IllegalArgumentException("Invalid requiredStyle in config: ${configString("requiredStyle")}")
            }
        } else requiredStyle

        // enumStyle may also be provided by config file when not passed on CLI
        val enumStyleFinal = if (options.enumStyleOption == null && configString("enumStyle") != null) {
            when (configString("enumStyle")!!.lowercase()) {
                "inline" -> EnumStyle.INLINE
                "note" -> EnumStyle.NOTE
                "class" -> EnumStyle.CLASS
                else -> throw IllegalArgumentException("Invalid enumStyle in config: ${configString("enumStyle")}")
            }
        } else enumStyle

        return Preferences(
            arraysAsRelation = arraysPreference,
            enumStyle = enumStyleFinal,
            useEnglishSingularizer = options.useEnglishSingularizer,
            showInheritedFields = options.showInheritedFields,
            requiredFieldStyle = requiredStyleFinal
        )
    }
}
