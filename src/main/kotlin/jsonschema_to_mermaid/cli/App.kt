package jsonschema_to_mermaid.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.versionOption
import com.github.ajalt.clikt.parameters.types.path
import com.github.ajalt.clikt.parameters.options.flag
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
    private val arraysAsRelation: Boolean by option(
        "--arrays-as-relation",
        help = "Render arrays as relationships (default: true)"
    ).flag(default = true)
    private val arraysInline: Boolean by option(
        "--arrays-inline",
        help = "Render arrays as inline fields (overrides --arrays-as-relation)"
    ).flag(default = false)
    private val requiredStyleOption: String? by option(
        "--required-style",
        help = "Required field marker style: plus (default), none, or suffix-q"
    )
    private val useEnglishSingularizer: Boolean by option(
        "--english-singularizer",
        help = "Use English singularization for array item names (default: true). Disable for non-English diagrams."
    ).flag(default = true)
    private val showInheritedFields: Boolean by option(
        "--show-inherited-fields",
        help = "Display inherited properties on child classes (default: hidden)"
    ).flag(default = false)

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
        val options = CliOptions(
            sourceFileOption = sourceFileOption,
            sourceDirOption = sourceDirOption,
            sourcePath = sourcePath,
            outputPathArg = outputPathArg,
            outputPath = outputPath,
            noClassDiagramHeader = noClassDiagramHeader,
            enumStyleOption = enumStyleOption,
            useEnglishSingularizer = useEnglishSingularizer,
            showInheritedFields = showInheritedFields,
            arraysAsRelation = arraysAsRelation,
            arraysInline = arraysInline,
            requiredStyleOption = requiredStyleOption
        )
        val cliService = CliService(
            options
        ) { msg, _ -> echo(msg) }
        cliService.execute()
    }
}
