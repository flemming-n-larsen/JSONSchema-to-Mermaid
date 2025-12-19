package jsonschema_to_mermaid.cli

import jsonschema_to_mermaid.schema_files.SchemaFileInfo
import java.nio.file.Path

/**
 * Handles diagnostic logging for CLI operations.
 *
 * This class follows the Single Responsibility Principle by only handling
 * diagnostic output.
 */
class DiagnosticLogger(
    private val echo: (message: String, toStderr: Boolean) -> Unit
) {

    /**
     * Logs source resolution diagnostics.
     */
    fun logSourceLocation(location: SourceResolver.SourceLocation) {
        echo("Using source directory: ${location.directory}", true)

        if (location.fileName != null) {
            echo("Using source file: ${location.fileName}", true)
        }

        if (location.usedPositionalArgument) {
            echo("Used positional <source> argument.", true)
        }
    }

    /**
     * Logs resolved source paths with their details.
     */
    fun logResolvedPaths(sources: Set<Path>) {
        echo("Resolved source paths:", true)

        sources.forEach { path ->
            val absolutePath = formatAbsolutePath(path)
            val exists = path.toFile().exists()
            val isDirectory = path.toFile().isDirectory

            echo(" - '$path' -> abs='$absolutePath', exists=$exists, isDirectory=$isDirectory", true)
        }
    }

    /**
     * Logs information about parsed schemas.
     */
    fun logSchemas(schemas: List<SchemaFileInfo>) {
        val schemaNames = schemas.joinToString(",") { it.filename ?: "<unknown>" }
        echo("Read ${schemas.size} schema(s): $schemaNames", true)
    }

    /**
     * Logs an error message.
     */
    fun logError(message: String) = echo(message, true)

    /**
     * Logs an error with styled output.
     */
    fun logStyledError(message: String) =
        echo("Failed to generate Mermaid: \u001B[31m$message\u001B[0m", true)

    private fun formatAbsolutePath(path: Path): String =
        runCatching { path.toAbsolutePath().toString() }.getOrDefault("<invalid>")
}

