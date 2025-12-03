package jsonschema_to_mermaid.schema_files

import com.google.gson.GsonBuilder
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.io.FileReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Path
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

object RefResolver {
    private val gson = GsonBuilder().create()
    private val yaml = Yaml()
    private val httpCache = ConcurrentHashMap<String, String>()
    private val httpTimeout = Duration.ofSeconds(5)

    /**
     * Resolves a $ref string to a parsed schema map (JSON/YAML).
     * Supports file paths and HTTP(S) URLs.
     */
    fun resolve(ref: String, baseDir: Path): Map<String, Any> {
        return when {
            ref.startsWith("http://") || ref.startsWith("https://") -> fetchHttp(ref)
            else -> fetchFile(ref, baseDir)
        }
    }

    private fun fetchFile(ref: String, baseDir: Path): Map<String, Any> {
        val file = baseDir.resolve(ref).normalize().toFile()
        if (!file.exists()) throw IllegalArgumentException("File not found: $file for $ref")
        return parseFile(file)
    }

    private fun fetchHttp(ref: String): Map<String, Any> {
        val text = httpCache.getOrPut(ref) {
            val url = URL(ref)
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = httpTimeout.toMillis().toInt()
            conn.readTimeout = httpTimeout.toMillis().toInt()
            conn.inputStream.bufferedReader().use { it.readText() }
        }
        return parseText(ref, text)
    }

    private fun parseFile(file: File): Map<String, Any> {
        val ext = file.extension.lowercase()
        FileReader(file).use { reader ->
            return when (ext) {
                "json" -> gson.fromJson(reader, Map::class.java) as Map<String, Any>
                "yaml", "yml" -> yaml.load(reader) as Map<String, Any>
                else -> throw IllegalArgumentException("Unsupported file extension: $ext")
            }
        }
    }

    private fun parseText(ref: String, text: String): Map<String, Any> {
        return when {
            ref.endsWith(".json") -> gson.fromJson(text, Map::class.java) as Map<String, Any>
            ref.endsWith(".yaml") || ref.endsWith(".yml") -> yaml.load(text) as Map<String, Any>
            else -> throw IllegalArgumentException("Unsupported remote schema type for $ref")
        }
    }
}

