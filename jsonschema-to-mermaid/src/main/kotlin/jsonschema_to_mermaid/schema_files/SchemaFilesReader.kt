package jsonschema_to_mermaid.schema_files

import com.google.gson.GsonBuilder
import jsonschema_to_mermaid.exception.FileFormatException
import jsonschema_to_mermaid.exception.InheritanceCycleException
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

    private fun readSchema(path: Path): Schema = readSchema(path.toAbsolutePath().normalize(), mutableSetOf())

    private fun readSchema(path: Path, visiting: MutableSet<Path>): Schema {
        val abs = path.toAbsolutePath().normalize()
        if (!abs.toFile().exists()) throw FileFormatException("Schema file not found: ${abs.fileName}")
        if (!visiting.add(abs)) {
            throw InheritanceCycleException("Cycle detected while resolving extends: ${visiting.joinToString(" -> ") { it.fileName.toString() }} -> ${abs.fileName}")
        }
        val schema = when (getFileExtension(abs)) {
            "json" -> parseJsonSchema(abs)
            "yaml", "yml" -> parseYamlSchema(abs)
            else -> throw FileFormatException("Unsupported schema file type: ${abs.fileName}")
        }
        val resolved = resolveExtends(schema, abs, visiting)
        visiting.remove(abs)
        return resolved
    }

    // Parse only – no extends resolution
    private fun parseJsonSchema(path: Path): Schema {
        return try {
            FileReader(path.toFile()).use { fileReader ->
                gson.fromJson(fileReader, Schema::class.java) ?: throw FileFormatException("Could not parse JSON schema file: null result")
            }
        } catch (e: Exception) {
            throw FileFormatException("Could not parse JSON schema file", e)
        }
    }

    private fun parseYamlSchema(path: Path): Schema {
        return try {
            FileReader(path.toFile()).use { fileReader ->
                val yamlObj: Any? = yaml.load(fileReader)
                if (yamlObj !is Map<*, *>) {
                    throw FileFormatException("YAML root is not a mapping for file: ${path.fileName}")
                }
                @Suppress("UNCHECKED_CAST")
                val yamlMap = yamlObj as Map<String, Any>
                gson.fromJson(gson.toJson(yamlMap), Schema::class.java) ?: throw FileFormatException("Could not parse YAML schema file: null result")
            }
        } catch (e: Exception) {
            throw FileFormatException("Could not parse YAML schema file", e)
        }
    }

    // Updated resolveExtends using shared visited set
    private fun resolveExtends(schema: Schema, path: Path, visiting: MutableSet<Path>): Schema {
        val extends = schema.extends ?: return schema
        val refPath = when (extends) {
            is Extends.Ref -> path.parent.resolve(extends.ref)
            is Extends.Object -> path.parent.resolve(extends.ref)
        }.toAbsolutePath().normalize()
        val baseSchema = readSchema(refPath, visiting) // recursive with same visited set
        val baseOwnProps = baseSchema.properties?.keys ?: emptySet()
        val baseInherited = baseSchema.inheritedPropertyNames ?: emptyList()
        val childOwnProps = schema.properties?.keys ?: emptySet()
        val inheritedForChild = ((baseOwnProps + baseInherited) - childOwnProps).toList().sorted()
        return schema.copy(
            properties = mergeMaps(baseSchema.properties, schema.properties),
            required = mergeLists(baseSchema.required, schema.required),
            definitions = mergeMaps(baseSchema.definitions, schema.definitions),
            inheritedPropertyNames = inheritedForChild,
        )
    }

    private fun <K, V> mergeMaps(base: Map<K, V>?, override: Map<K, V>?): Map<K, V>? {
        if (base == null && override == null) return null
        if (base == null) return override
        if (override == null) return base
        return base + override
    }
    private fun <T> mergeLists(base: List<T>?, override: List<T>?): List<T>? {
        if (base == null && override == null) return null
        if (base == null) return override
        if (override == null) return base
        return (base + override).distinct()
    }

    private fun getFileExtension(path: Path): String {
        val fileName = path.fileName.toString()
        val lastDot = fileName.lastIndexOf('.')
        return if (lastDot > 0) fileName.substring(lastDot + 1).lowercase() else ""
    }
}
