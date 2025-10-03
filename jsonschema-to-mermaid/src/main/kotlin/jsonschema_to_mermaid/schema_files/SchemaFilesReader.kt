package jsonschema_to_mermaid.schema_files

import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import jsonschema_to_mermaid.exception.FileFormatException
import jsonschema_to_mermaid.jsonschema.Schema
import org.yaml.snakeyaml.Yaml
import java.io.FileReader
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.streams.toList

object SchemaFilesReader {

    private val gson = GsonBuilder().create()
    private val yaml = Yaml()

    fun readSchemas(source: Set<Path>): List<SchemaFileInfo> {
        return collectAllFiles(source).mapNotNull { filepath ->
            try {
                val schema = readSchema(filepath)
                SchemaFileInfo(filepath.name, schema)
            } catch (e: Exception) {
                // Log the problem and skip this file rather than failing the whole run
                System.err.println("Warning: skipping '${filepath}': ${e.message}")
                null
            }
        }
    }

    private fun collectAllFiles(source: Set<Path>): Set<Path> =
        source.flatMap { file ->
            if (file.isDirectory()) {
                collectAllFiles(Files.list(file).toList().toSet())
            } else if (setOf("json", "yaml", "yml").contains(getFileExtension(file))) {
                setOf(file)
            } else {
                setOf()
            }
        }.toSet()

    private fun readSchema(path: Path): Schema =
        when (getFileExtension(path)) {
            "json" -> readJsonSchema(path)
            "yaml", "yml" -> readYamlSchema(path)
            else -> throw FileFormatException("Unsupported schema file type: ${path.fileName}")
        }

    private fun getFileExtension(path: Path): String {
        val fileName = path.fileName.toString()
        val lastIndexOf = fileName.lastIndexOf(".")
        return if (lastIndexOf > 0) fileName.substring(lastIndexOf + 1).lowercase() else ""
    }

    private fun readJsonSchema(path: Path): Schema {
        try {
            FileReader(path.toFile()).use { fileReader ->
                return gson.fromJson(fileReader, Schema::class.java)
            }
        } catch (e: JsonSyntaxException) {
            throw FileFormatException("Could not parse JSON schema file", e)
        }
    }

    private fun readYamlSchema(path: Path): Schema {
        try {
            FileReader(path.toFile()).use { fileReader ->
                val yamlObj: Any? = yaml.load(fileReader)
                if (yamlObj !is Map<*, *>) {
                    throw FileFormatException("YAML root is not a mapping for file: ${path.fileName}")
                }
                @Suppress("UNCHECKED_CAST")
                val yamlMap = yamlObj as Map<String, Any>
                return gson.fromJson(gson.toJson(yamlMap), Schema::class.java)
            }
        } catch (e: ClassCastException) {
            throw FileFormatException("Could not parse YAML schema file", e)
        }
    }
}
