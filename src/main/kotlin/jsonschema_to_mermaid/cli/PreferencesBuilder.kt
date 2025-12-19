package jsonschema_to_mermaid.cli

import com.google.gson.JsonObject
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
            requiredFieldStyle = resolveRequiredStyle(config)
        )
    }

    private fun resolveArraysAsRelation(config: JsonObject?): Boolean {
        // CLI --arrays option takes highest precedence
        val cliValue = options.arraysOption
        if (cliValue != null) {
            return parseArraysValue(cliValue, "CLI option")
        }

        // Check config file
        val configValue = configFileResolver.getString(config, "arrays")
        if (configValue != null) {
            return parseArraysValue(configValue, "config file")
        }

        // Default: arrays as relation
        return true
    }

    private fun parseArraysValue(value: String, source: String): Boolean {
        return when (value.lowercase()) {
            "inline" -> false
            "relation" -> true
            else -> throw InvalidOptionException("Invalid arrays value in $source: $value")
        }
    }

    private fun resolveEnumStyle(config: JsonObject?): EnumStyle {
        // CLI option takes precedence
        if (options.enumStyleOption != null) {
            return parseEnumStyle(options.enumStyleOption, "CLI option")
        }

        // Check config file
        val configValue = configFileResolver.getString(config, "enumStyle")
        if (configValue != null) {
            return parseEnumStyle(configValue, "config file")
        }

        // Default
        return EnumStyle.INLINE
    }

    private fun parseEnumStyle(value: String, source: String): EnumStyle {
        return when (value.lowercase()) {
            "inline" -> EnumStyle.INLINE
            "note" -> EnumStyle.NOTE
            "class" -> EnumStyle.CLASS
            else -> throw InvalidOptionException("Invalid enumStyle in $source: $value")
        }
    }

    private fun resolveRequiredStyle(config: JsonObject?): RequiredFieldStyle {
        // CLI option takes precedence
        if (options.requiredStyleOption != null) {
            return parseRequiredStyle(options.requiredStyleOption, "CLI option")
        }

        // Check config file
        val configValue = configFileResolver.getString(config, "requiredStyle")
        if (configValue != null) {
            return parseRequiredStyle(configValue, "config file")
        }

        // Default
        return RequiredFieldStyle.PLUS
    }

    private fun parseRequiredStyle(value: String, source: String): RequiredFieldStyle {
        return when (value.lowercase()) {
            "plus" -> RequiredFieldStyle.PLUS
            "none" -> RequiredFieldStyle.NONE
            "suffix-q" -> RequiredFieldStyle.SUFFIX_Q
            else -> throw InvalidOptionException("Invalid requiredStyle in $source: $value")
        }
    }
}

/**
 * Exception thrown when an invalid option value is encountered.
 */
class InvalidOptionException(message: String) : IllegalArgumentException(message)
