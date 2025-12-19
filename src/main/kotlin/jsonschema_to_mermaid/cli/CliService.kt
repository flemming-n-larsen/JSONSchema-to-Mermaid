package jsonschema_to_mermaid.cli

import jsonschema_to_mermaid.diagram.MermaidGenerator
import jsonschema_to_mermaid.diagram.Preferences
import jsonschema_to_mermaid.schema_files.SchemaFileInfo
import jsonschema_to_mermaid.schema_files.SchemaFilesReader
import java.nio.file.Path

/**
 * Orchestrates the CLI workflow by coordinating specialized components.
 *
 * This class follows the Single Responsibility Principle by delegating
 * specific tasks to focused collaborators and only handling the overall flow.
 */
class CliService(
    private val options: CliOptions,
    private val echo: (String, Boolean) -> Unit,
    private val sourceResolver: SourceResolver = SourceResolver(options),
    private val configFileResolver: ConfigFileResolver = ConfigFileResolver(),
    private val preferencesBuilder: PreferencesBuilder = PreferencesBuilder(options, configFileResolver),
    private val diagnosticLogger: DiagnosticLogger = DiagnosticLogger(echo),
    private val outputWriter: OutputWriter = OutputWriter(echo)
) {

    /**
     * Executes the main CLI workflow:
     * 1. Resolve source files
     * 2. Read schemas
     * 3. Build preferences
     * 4. Generate Mermaid output
     * 5. Write output
     */
    fun execute() {
        val sourceLocation = resolveSourceLocation() ?: return
        val sourceFiles = resolveSourceFiles(sourceLocation) ?: return
        val schemas = readSchemas(sourceFiles) ?: return
        val preferences = buildPreferences(sourceLocation.directory) ?: return
        val output = generateMermaid(schemas, preferences) ?: return
        writeOutput(output)
    }

    private fun resolveSourceLocation(): SourceResolver.SourceLocation? {
        val location = sourceResolver.resolveSourceLocation()
        diagnosticLogger.logSourceLocation(location)
        return location
    }

    private fun resolveSourceFiles(location: SourceResolver.SourceLocation): Set<Path>? {
        val sources = sourceResolver.resolveSourceFiles(location)
        diagnosticLogger.logResolvedPaths(sources)

        if (sources.isEmpty()) {
            diagnosticLogger.logError("Error: No schema files found to process.")
            return null
        }

        return sources
    }

    private fun readSchemas(sourceFiles: Set<Path>): List<SchemaFileInfo>? {
        return try {
            val schemas = SchemaFilesReader.readSchemas(sourceFiles)
            diagnosticLogger.logSchemas(schemas)
            schemas
        } catch (e: Exception) {
            diagnosticLogger.logError("Failed to read schemas: ${e.message}")
            null
        }
    }

    private fun buildPreferences(sourceDirectory: Path): Preferences? {
        return try {
            val configPath = configFileResolver.resolveConfigPath(options.configFile, sourceDirectory)
            val config = configPath?.let { configFileResolver.parseConfig(it) }
            preferencesBuilder.build(config)
        } catch (e: ConfigParseException) {
            diagnosticLogger.logError(e.message ?: "Config parse error")
            System.err.println(e.message)
            null
        } catch (e: InvalidOptionException) {
            diagnosticLogger.logError(e.message ?: "Invalid option")
            System.err.println(e.message)
            null
        }
    }

    private fun generateMermaid(schemas: List<SchemaFileInfo>, preferences: Preferences): String? {
        return try {
            MermaidGenerator.generate(
                schemaFiles = schemas,
                noClassDiagramHeader = options.noClassDiagramHeader,
                preferences = preferences
            )
        } catch (e: Exception) {
            diagnosticLogger.logStyledError(e.message ?: "Unknown error")
            e.printStackTrace()
            null
        }
    }

    private fun writeOutput(output: String) {
        try {
            outputWriter.write(options.effectiveOutputPath, output)
        } catch (e: OutputWriteException) {
            diagnosticLogger.logError(e.message ?: "Output write error")
        }
    }
}
