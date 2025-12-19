package jsonschema_to_mermaid.cli

import com.google.gson.JsonObject
import jsonschema_to_mermaid.diagram.AllOfMode
import jsonschema_to_mermaid.diagram.EnumStyle
import jsonschema_to_mermaid.diagram.Preferences
import jsonschema_to_mermaid.diagram.RequiredFieldStyle

/**
 * Builds Preferences from CLI options and configuration file.
 *
 * This class follows the Single Responsibility Principle by only handling
 * the construction of Preferences objects.
 */
class PreferencesBuilder(
    private val options: CliOptions,
    private val configFileResolver: ConfigFileResolver = ConfigFileResolver()
) {

    /**
     * Builds Preferences by merging CLI options with config file settings.
     * CLI options take precedence over config file settings.
     *
     * @param config Optional parsed configuration object
     * @return Built Preferences object
     * @throws InvalidOptionException if any option value is invalid
     */
    fun build(config: JsonObject? = null): Preferences {
        return Preferences(
            arraysAsRelation = resolveArraysAsRelation(config),
            enumStyle = resolveEnumStyle(config),
            useEnglishSingularizer = options.useEnglishSingularizer,
            showInheritedFields = options.showInheritedFields,
            requiredFieldStyle = resolveRequiredStyle(config),
            allOfMode = resolveAllOfMode(config)
        )
    }

    private fun resolveArraysAsRelation(config: JsonObject?): Boolean =
        options.arraysOption?.let { parseArraysValue(it, "CLI option") }
            ?: configFileResolver.getString(config, "arrays")?.let { parseArraysValue(it, "config file") }
            ?: true // Default: arrays as relation

    private fun parseArraysValue(value: String, source: String): Boolean = when (value.lowercase()) {
        "inline" -> false
        "relation" -> true
        else -> throw InvalidOptionException("Invalid arrays value in $source: $value")
    }

    private fun resolveEnumStyle(config: JsonObject?): EnumStyle =
        options.enumStyleOption?.let { parseEnumStyle(it, "CLI option") }
            ?: configFileResolver.getString(config, "enumStyle")?.let { parseEnumStyle(it, "config file") }
            ?: EnumStyle.INLINE

    private fun parseEnumStyle(value: String, source: String): EnumStyle = when (value.lowercase()) {
        "inline" -> EnumStyle.INLINE
        "note" -> EnumStyle.NOTE
        "class" -> EnumStyle.CLASS
        else -> throw InvalidOptionException("Invalid enumStyle in $source: $value")
    }

    private fun resolveRequiredStyle(config: JsonObject?): RequiredFieldStyle =
        options.requiredStyleOption?.let { parseRequiredStyle(it, "CLI option") }
            ?: configFileResolver.getString(config, "requiredStyle")?.let { parseRequiredStyle(it, "config file") }
            ?: RequiredFieldStyle.PLUS

    private fun parseRequiredStyle(value: String, source: String): RequiredFieldStyle = when (value.lowercase()) {
        "plus" -> RequiredFieldStyle.PLUS
        "none" -> RequiredFieldStyle.NONE
        "suffix-q" -> RequiredFieldStyle.SUFFIX_Q
        else -> throw InvalidOptionException("Invalid requiredStyle in $source: $value")
    }

    private fun resolveAllOfMode(config: JsonObject?): AllOfMode =
        options.allOfModeOption?.let { parseAllOfMode(it, "CLI option") }
            ?: configFileResolver.getString(config, "allOfMode")?.let { parseAllOfMode(it, "config file") }
            ?: AllOfMode.MERGE

    private fun parseAllOfMode(value: String, source: String): AllOfMode = when (value.lowercase()) {
        "merge" -> AllOfMode.MERGE
        "inherit" -> AllOfMode.INHERIT
        "compose" -> AllOfMode.COMPOSE
        else -> throw InvalidOptionException("Invalid allOfMode in $source: $value")
    }
}

/**
 * Exception thrown when an invalid option value is encountered.
 */
class InvalidOptionException(message: String) : IllegalArgumentException(message)
