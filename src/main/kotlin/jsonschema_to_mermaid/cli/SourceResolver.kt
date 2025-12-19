package jsonschema_to_mermaid.cli

import java.nio.file.Path
import java.nio.file.Paths

/**
 * Resolves source files and directories from CLI options.
 *
 * This class follows the Single Responsibility Principle by only handling
 * source resolution logic.
 */
class SourceResolver(private val options: CliOptions) {

    /**
     * Result of resolving source directory and file.
     */
    data class SourceLocation(
        val directory: Path,
        val fileName: String?,
        val usedPositionalArgument: Boolean
    )

    /**
     * Resolves the source directory and optional file name from CLI options.
     *
     * Priority:
     * 1. Explicit --source-dir and --source options
     * 2. Positional source path argument
     * 3. Current working directory as fallback
     */
    fun resolveSourceLocation(): SourceLocation {
        return when {
            hasExplicitSourceOptions() -> resolveFromExplicitOptions()
            hasPositionalSourcePath() -> resolveFromPositionalPath()
            else -> resolveFromCurrentDirectory()
        }
    }

    /**
     * Resolves all source schema files to process.
     *
     * @param location The resolved source location
     * @return Set of paths to schema files
     */
    fun resolveSourceFiles(location: SourceLocation): Set<Path> {
        return if (location.fileName != null) {
            resolveSpecificFile(location.directory, location.fileName)
        } else {
            resolveAllSchemaFilesInDirectory(location.directory)
        }
    }

    private fun hasExplicitSourceOptions(): Boolean {
        return options.sourceDirOption != null || options.sourceFileOption != null
    }

    private fun hasPositionalSourcePath(): Boolean {
        return options.sourcePath != null
    }

    private fun resolveFromExplicitOptions(): SourceLocation {
        val directory = options.sourceDirOption ?: currentWorkingDirectory()
        return SourceLocation(
            directory = directory,
            fileName = options.sourceFileOption,
            usedPositionalArgument = false
        )
    }

    private fun resolveFromPositionalPath(): SourceLocation {
        val sourcePath = options.sourcePath!!
        val file = sourcePath.toFile()

        return if (file.exists() && file.isDirectory) {
            SourceLocation(
                directory = sourcePath,
                fileName = null,
                usedPositionalArgument = true
            )
        } else {
            SourceLocation(
                directory = sourcePath.parent ?: currentWorkingDirectory(),
                fileName = sourcePath.fileName.toString(),
                usedPositionalArgument = true
            )
        }
    }

    private fun resolveFromCurrentDirectory(): SourceLocation {
        return SourceLocation(
            directory = currentWorkingDirectory(),
            fileName = null,
            usedPositionalArgument = false
        )
    }

    private fun resolveSpecificFile(directory: Path, fileName: String): Set<Path> {
        val file = directory.resolve(fileName)
        return if (file.toFile().exists()) setOf(file) else emptySet()
    }

    private fun resolveAllSchemaFilesInDirectory(directory: Path): Set<Path> {
        val files = directory.toFile().listFiles { file ->
            file.isFile && isSchemaFile(file.name) && !isConfigFile(file.name)
        } ?: emptyArray()

        return files.map { it.toPath() }.toSet()
    }

    private fun isSchemaFile(fileName: String): Boolean {
        return fileName.endsWith(".json") ||
                fileName.endsWith(".yaml") ||
                fileName.endsWith(".yml")
    }

    private fun isConfigFile(fileName: String): Boolean {
        return fileName in CONFIG_FILE_NAMES
    }

    private fun currentWorkingDirectory(): Path {
        return Paths.get("").toAbsolutePath()
    }

    companion object {
        private val CONFIG_FILE_NAMES = setOf("js2m.json", ".js2mrc", ".js2m.json")
    }
}

