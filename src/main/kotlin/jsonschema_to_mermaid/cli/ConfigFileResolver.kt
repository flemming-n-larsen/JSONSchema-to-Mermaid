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
            ?: findConfigInParentDirs(sourceDirectory)
            ?: findUserLevelConfig()
    }

    /**
     * Parses the configuration file and returns a JsonObject.
     *
     * @param configPath Path to the configuration file
     * @return Parsed JsonObject (empty object if file is empty)
     * @throws ConfigParseException if the file contains invalid JSON
     */
    fun parseConfig(configPath: Path): JsonObject {
        return try {
            val content = String(Files.readAllBytes(configPath))
            if (content.isBlank()) {
                return JsonObject()
            }
            Gson().fromJson(content, JsonObject::class.java) ?: JsonObject()
        } catch (e: JsonSyntaxException) {
            throw ConfigParseException("Invalid JSON in config file: ${e.message}", e)
        }
    }

    /**
     * Safely retrieves a string value from the config object.
     * Performs case-insensitive key lookup.
     */
    fun getString(config: JsonObject?, key: String): String? {
        if (config == null) return null

        // Try exact match first
        val exactMatch = config.get(key)
        if (exactMatch != null) {
            return exactMatch.asString
        }

        // Try case-insensitive match
        val lowerKey = key.lowercase()
        for (entry in config.entrySet()) {
            if (entry.key.lowercase() == lowerKey) {
                return entry.value.asString
            }
        }

        return null
    }

    private fun findConfigInParentDirs(startDir: Path): Path? {
        var dir: Path? = startDir.toAbsolutePath()
        while (dir != null) {
            for (configName in PROJECT_CONFIG_NAMES) {
                val candidate = dir.resolve(configName)
                if (isValidConfigFile(candidate)) {
                    return candidate
                }
            }
            dir = dir.parent
        }
        return null
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
