package jsonschema_to_mermaid.cli

import java.nio.file.Path

/**
 * Immutable data class encapsulating all CLI options.
 *
 * This class follows the Single Responsibility Principle by only holding
 * CLI option data without any behavior.
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
    val arraysOption: String? = null,
    val requiredStyleOption: String? = null
) {
    /**
     * Returns the effective output path, preferring the option over the argument.
     */
    val effectiveOutputPath: Path?
        get() = outputPath ?: outputPathArg
}
