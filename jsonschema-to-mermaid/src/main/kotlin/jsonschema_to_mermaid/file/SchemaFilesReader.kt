package jsonschema_to_mermaid.file

import com.google.gson.GsonBuilder
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

    fun readSchemas(source: Set<Path>): Collection<SchemaFileInfo> =
        collectAllFiles(source).map { filepath ->
            val schema = readSchema(filepath)
            SchemaFileInfo(filepath.name, schema)
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
            else -> throw UnsupportedOperationException("Unsupported file type: ${path.fileName}")
        }

    private fun getFileExtension(path: Path): String {
        val fileName = path.fileName.toString()
        val lastIndexOf = fileName.lastIndexOf(".")
        return if (lastIndexOf > 0) fileName.substring(lastIndexOf + 1).lowercase() else ""
    }

    private fun readJsonSchema(path: Path): Schema {
        FileReader(path.toFile()).use { fileReader ->
            return gson.fromJson(fileReader, Schema::class.java)
        }
    }

    private fun readYamlSchema(path: Path): Schema {
        FileReader(path.toFile()).use { fileReader ->
            val yaml: Map<String, Any> = yaml.load(fileReader)
            return gson.fromJson(gson.toJson(yaml), Schema::class.java)
        }
    }
}