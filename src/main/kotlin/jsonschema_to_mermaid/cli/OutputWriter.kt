package jsonschema_to_mermaid.cli

import java.nio.file.Path

/**
 * Handles writing output to files or stdout.
 *
 * This class follows the Single Responsibility Principle by only handling
 * output writing operations.
 */
class OutputWriter(
    private val echo: (message: String, toStderr: Boolean) -> Unit
) {

    /**
     * Writes output to the specified path, or prints to stdout if path is null.
     *
     * @param outputPath Optional output file path
     * @param content The content to write
     */
    fun write(outputPath: Path?, content: String) {
        if (outputPath != null) {
            writeToFile(outputPath, content)
        } else {
            writeToStdout(content)
        }
    }

    private fun writeToFile(outputPath: Path, content: String) {
        try {
            outputPath.toFile().writeText(content)
        } catch (e: Exception) {
            throw OutputWriteException("Failed to write output to $outputPath: ${e.message}", e)
        }
    }

    private fun writeToStdout(content: String) {
        print(content)
    }
}

/**
 * Exception thrown when writing output fails.
 */
class OutputWriteException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

