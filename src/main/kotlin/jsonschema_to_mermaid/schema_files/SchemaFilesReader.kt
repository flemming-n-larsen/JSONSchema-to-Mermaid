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

/**
 * Context for reading schemas, encapsulating the current visiting set for cycle detection.
 */
private data class SchemaReadContext(val visiting: MutableSet<Path> = mutableSetOf())

object SchemaFilesReader {

    private val gson = GsonBuilder()
        .registerTypeAdapter(Extends::class.java, ExtendsTypeAdapter())
        .create()
    private val yaml = Yaml()

    /**
     * Reads all schemas from the given set of source paths (files or directories).
     */
    fun readSchemas(sourcePaths: Set<Path>): List<SchemaFileInfo> =
        collectAllFiles(sourcePaths).map { filepath ->
            val schema = readSchema(filepath)
            SchemaFileInfo(filepath.name, schema)
        }

    /**
     * Recursively collects all schema files (json/yaml/yml) from the given paths.
     */
    private fun collectAllFiles(sourcePaths: Set<Path>): Set<Path> =
        sourcePaths.flatMap { file ->
            when {
                file.isDirectory() -> collectAllFiles(Files.list(file).toList().toSet())
                isSchemaFile(file) -> setOf(file)
                else -> emptySet()
            }
        }.toSet()

    private fun isSchemaFile(file: Path): Boolean =
        setOf("json", "yaml", "yml").contains(getFileExtension(file))

    /**
     * Reads a schema from the given path, handling inheritance and required fields.
     */
    private fun readSchema(path: Path): Schema =
        readSchemaInternal(path.toAbsolutePath().normalize(), SchemaReadContext())

    private fun readSchemaInternal(path: Path, context: SchemaReadContext): Schema {
        val abs = path.toAbsolutePath().normalize()
        if (!abs.toFile().exists()) throw FileFormatException("Schema file not found: $abs")
        if (!context.visiting.add(abs)) {
            throw InheritanceCycleException(formatCycleMessage(context.visiting, abs))
        }
        val (schema, rootMap) = parseSchemaWithMap(abs)
        val resolved = resolveExtends(schema, abs, context)
        context.visiting.remove(abs)
        // Post-process definitions & top-level properties to ensure required fields are set from the original map (recursively)
        val fixedDefinitions = resolved.definitions?.mapValues { (defName, defSchema) ->
            val defMap = (rootMap["definitions"] as? Map<*, *>)?.get(defName) as? Map<*, *>
            val requiredStrings = (defMap?.get("required") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            val fixedProps = defSchema.properties?.mapValues { (propName, prop) ->
                val propMap = (defMap?.get("properties") as? Map<*, *>)?.get(propName) as? Map<*, *>
                fixRequiredRecursively(prop, propMap) ?: prop
            }
            defSchema.copy(
                properties = fixedProps,
                required = requiredStrings
            )
        }
        val fixedTopLevelProperties = resolved.properties?.mapValues { (propName, prop) ->
            val propMap = (rootMap["properties"] as? Map<*, *>)?.get(propName) as? Map<*, *>
            fixRequiredRecursively(prop, propMap) ?: prop
        }
        return resolved.copy(definitions = fixedDefinitions, properties = fixedTopLevelProperties)
    }

    /**
     * Parses a schema file (json/yaml/yml) and returns the Schema object and the root map.
     */
    private fun parseSchemaWithMap(path: Path): Pair<Schema, Map<String, Any>> =
        when (getFileExtension(path)) {
            "json" -> parseJsonSchemaWithMap(path)
            "yaml", "yml" -> parseYamlSchemaWithMap(path)
            else -> throw FileFormatException("Unsupported schema file type '${path.fileName}' at path: $path")
        }

    private fun parseJsonSchemaWithMap(path: Path): Pair<Schema, Map<String, Any>> =
        try {
            FileReader(path.toFile()).use { fileReader ->
                @Suppress("UNCHECKED_CAST")
                val gsonMap = gson.fromJson(fileReader, Map::class.java) as Map<*, *>
                val stringKeyMap = gsonMap.entries.associate { (k, v) -> k.toString() to v } // Map<String, Any?>
                @Suppress("UNCHECKED_CAST")
                val safeStringKeyMap = stringKeyMap.filterValues { it != null } as Map<String, Any>
                val schema = gson.fromJson(gson.toJson(safeStringKeyMap), Schema::class.java)
                    ?: throw FileFormatException("Could not parse JSON schema file: null result")
                schema to safeStringKeyMap
            }
        } catch (e: Exception) {
            throw FileFormatException("Could not parse JSON schema file", e)
        }

    private fun parseYamlSchemaWithMap(path: Path): Pair<Schema, Map<String, Any>> =
        try {
            FileReader(path.toFile()).use { fileReader ->
                val yamlObj: Any? = yaml.load(fileReader)
                if (yamlObj !is Map<*, *>) {
                    throw FileFormatException("YAML root is not a mapping for file: ${path.fileName}")
                }
                @Suppress("UNCHECKED_CAST")
                val yamlMap = yamlObj as Map<String, Any>
                val schema = gson.fromJson(gson.toJson(yamlMap), Schema::class.java)
                    ?: throw FileFormatException("Could not parse YAML schema file: null result")
                schema to yamlMap
            }
        } catch (e: Exception) {
            throw FileFormatException("Could not parse YAML schema file", e)
        }

    /**
     * Resolves inheritance (extends) for a schema, recursively merging base schemas.
     */
    private fun resolveExtends(schema: Schema, path: Path, context: SchemaReadContext): Schema {
        val extends = schema.extends ?: return schema
        val refPath = when (extends) {
            is Extends.Ref -> path.parent.resolve(extends.ref)
            is Extends.Object -> path.parent.resolve(extends.ref)
        }.toAbsolutePath().normalize()
        val baseSchema = readSchemaInternal(refPath, context)
        val baseOwnProps = baseSchema.properties?.keys ?: emptySet()
        val baseInherited = baseSchema.inheritedPropertyNames ?: emptyList()
        val childOwnProps = schema.properties?.keys ?: emptySet()
        val inheritedForChild = ((baseOwnProps + baseInherited) - childOwnProps).toList().sorted()
        return schema.copy(
            properties = mergeMaps(baseSchema.properties, schema.properties),
            required = mergeLists(baseSchema.required, schema.required) ?: emptyList(),
            definitions = mergeMaps(baseSchema.definitions, schema.definitions),
            inheritedPropertyNames = inheritedForChild,
        )
    }
}

/**
 * Recursively fixes the required fields for a property and its children, using the original map.
 */
private fun fixRequiredRecursively(
    property: jsonschema_to_mermaid.jsonschema.Property?,
    map: Map<*, *>?
): jsonschema_to_mermaid.jsonschema.Property? {
    if (property == null || map == null) return property
    val requiredList = (map["required"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
    val fixedProperties = property.properties?.mapValues { (k, v) ->
        val propMap = (map["properties"] as? Map<*, *>)?.get(k) as? Map<*, *>
        fixRequiredRecursively(v, propMap)
    }?.filterValues { it != null }?.mapValues { it.value!! }
    val fixedPatternProperties = property.patternProperties?.mapValues { (k, v) ->
        val patMap = (map["patternProperties"] as? Map<*, *>)?.get(k) as? Map<*, *>
        fixRequiredRecursively(v, patMap)
    }?.filterValues { it != null }?.mapValues { it.value!! }
    val fixedItems = property.items?.let {
        val itemsMap = map["items"] as? Map<*, *>
        fixRequiredRecursively(it, itemsMap)
    }
    val fixedAllOf = property.allOf?.mapIndexed { idx, p ->
        val allOfList = map["allOf"] as? List<*>
        val allOfMap = allOfList?.getOrNull(idx) as? Map<*, *>
        fixRequiredRecursively(p, allOfMap)
    }?.filterNotNull()
    val fixedOneOf = property.oneOf?.mapIndexed { idx, p ->
        val oneOfList = map["oneOf"] as? List<*>
        val oneOfMap = oneOfList?.getOrNull(idx) as? Map<*, *>
        fixRequiredRecursively(p, oneOfMap)
    }?.filterNotNull()
    val fixedAnyOf = property.anyOf?.mapIndexed { idx, p ->
        val anyOfList = map["anyOf"] as? List<*>
        val anyOfMap = anyOfList?.getOrNull(idx) as? Map<*, *>
        fixRequiredRecursively(p, anyOfMap)
    }?.filterNotNull()
    return property.copy(
        required = requiredList,
        properties = fixedProperties,
        patternProperties = fixedPatternProperties,
        items = fixedItems,
        allOf = fixedAllOf,
        oneOf = fixedOneOf,
        anyOf = fixedAnyOf
    )
}

private fun <K, V> mergeMaps(base: Map<K, V>?, override: Map<K, V>?): Map<K, V>? =
    when {
        base == null && override == null -> null
        base == null -> override
        override == null -> base
        else -> base + override
    }

private fun <T> mergeLists(base: List<T>?, override: List<T>?): List<T>? =
    when {
        base == null && override == null -> null
        base == null -> override
        override == null -> base
        else -> (base + override).distinct()
    }

private fun formatCycleMessage(visiting: Set<Path>, current: Path): String {
    val chain = (visiting.toList() + current).joinToString(" -> ") { it.toAbsolutePath().toString() }
    return "Inheritance cycle detected while resolving extends. Chain: $chain"
}

private fun getFileExtension(path: Path): String {
    val fileName = path.fileName.toString()
    val lastDot = fileName.lastIndexOf('.')
    return if (lastDot > 0) fileName.substring(lastDot + 1).lowercase() else ""
}
