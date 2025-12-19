package jsonschema_to_mermaid.cli

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Resolves and parses configuration files.
 *
 * This class follows the Single Responsibility Principle by only handling
 * configuration file discovery and parsing.
 */
class ConfigFileResolver {

    /**
     * Resolves the configuration file path using the following precedence:
     * 1. Explicit config file path (if provided)
     * 2. Project-level config files in the source directory
     * 3. User-level config files in the home directory
     *
     * @param explicitConfigPath Explicitly specified config file path
     * @param sourceDirectory The source directory to search for project-level configs
     * @return Resolved config file path, or null if none found
     */
    fun resolveConfigPath(explicitConfigPath: Path?, sourceDirectory: Path): Path? {
        return explicitConfigPath
            ?: findProjectLevelConfig(sourceDirectory)
            ?: findUserLevelConfig()
    }

    /**
     * Parses the configuration file and returns a JsonObject.
     *
     * @param configPath Path to the configuration file
     * @return Parsed JsonObject
     * @throws ConfigParseException if the file contains invalid JSON
     */
    fun parseConfig(configPath: Path): JsonObject {
        return try {
            val content = String(Files.readAllBytes(configPath))
            Gson().fromJson(content, JsonObject::class.java)
        } catch (e: JsonSyntaxException) {
            throw ConfigParseException("Invalid JSON in config file: ${e.message}", e)
        }
    }

    /**
     * Safely retrieves a string value from the config object.
     */
    fun getString(config: JsonObject?, key: String): String? {
        return config?.get(key)?.asString
    }

    private fun findProjectLevelConfig(sourceDirectory: Path): Path? {
        return PROJECT_CONFIG_NAMES
            .map { sourceDirectory.resolve(it) }
            .firstOrNull { isValidConfigFile(it) }
    }

    private fun findUserLevelConfig(): Path? {
        val homeDirectory = System.getProperty("user.home") ?: return null
        val homePath = Paths.get(homeDirectory)

        return USER_CONFIG_NAMES
            .map { homePath.resolve(it) }
            .firstOrNull { isValidConfigFile(it) }
    }

    private fun isValidConfigFile(path: Path): Boolean {
        val file = path.toFile()
        return file.exists() && file.isFile
    }

    companion object {
        private val PROJECT_CONFIG_NAMES = listOf("js2m.json", ".js2mrc")
        private val USER_CONFIG_NAMES = listOf(".js2m.json", ".js2mrc")
    }
}

/**
 * Exception thrown when config file parsing fails.
 */
class ConfigParseException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

