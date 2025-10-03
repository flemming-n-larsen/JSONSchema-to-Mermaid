package jsonschema_to_mermaid.schema_files

import com.google.gson.GsonBuilder
import jsonschema_to_mermaid.exception.FileFormatException
import jsonschema_to_mermaid.jsonschema.Extends
import jsonschema_to_mermaid.jsonschema.ExtendsTypeAdapter
import jsonschema_to_mermaid.jsonschema.Schema
import org.yaml.snakeyaml.Yaml
import java.io.FileReader
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.streams.toList

object SchemaFilesReader {

    private val gson = GsonBuilder()
        .registerTypeAdapter(Extends::class.java, ExtendsTypeAdapter())
        .create()
    private val yaml = Yaml()

    fun readSchemas(source: Set<Path>): List<SchemaFileInfo> {
        return collectAllFiles(source).map { filepath ->
            val schema = readSchema(filepath)
            SchemaFileInfo(filepath.name, schema)
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
                val schema = gson.fromJson(fileReader, Schema::class.java)
                if (schema == null) throw FileFormatException("Could not parse JSON schema file: null result")
                return resolveExtends(schema, path)
            }
        } catch (e: Exception) {
            throw FileFormatException("Could not parse JSON schema file", e)
        }
    }

    private fun readYamlSchema(path: Path): Schema {
        try {
            FileReader(path.toFile()).use { fileReader ->
                val yamlObj: Any? = yaml.load(fileReader)
                if (yamlObj !is Map<*, *>) {
                    throw FileFormatException("YAML root is not a mapping for file: \\${path.fileName}")
                }
                @Suppress("UNCHECKED_CAST")
                val yamlMap = yamlObj as Map<String, Any>
                val schema = gson.fromJson(gson.toJson(yamlMap), Schema::class.java)
                if (schema == null) throw FileFormatException("Could not parse YAML schema file: null result")
                return resolveExtends(schema, path)
            }
        } catch (e: Exception) {
            throw FileFormatException("Could not parse YAML schema file", e)
        }
    }

    // New: resolve extends property by merging referenced schema
    private fun resolveExtends(schema: Schema, path: Path): Schema {
        val extends = schema.extends
        if (extends == null) return schema
        val refPath = when (extends) {
            is jsonschema_to_mermaid.jsonschema.Extends.Ref -> path.parent.resolve(extends.ref)
            is jsonschema_to_mermaid.jsonschema.Extends.Object -> path.parent.resolve(extends.ref)
        }
        val baseSchema = readSchema(refPath)
        // Merge baseSchema into schema, with schema taking precedence
        return schema.copy(
            properties = mergeMaps(baseSchema.properties, schema.properties),
            required = mergeLists(baseSchema.required, schema.required),
            definitions = mergeMaps(baseSchema.definitions, schema.definitions),
            // extends is not propagated
        )
    }

    private fun <K, V> mergeMaps(base: Map<K, V>?, override: Map<K, V>?): Map<K, V>? {
        if (base == null && override == null) return null
        if (base == null) return override
        if (override == null) return base
        return base + override // override takes precedence
    }

    private fun <T> mergeLists(base: List<T>?, override: List<T>?): List<T>? {
        if (base == null && override == null) return null
        if (base == null) return override
        if (override == null) return base
        return (base + override).distinct()
    }
}
