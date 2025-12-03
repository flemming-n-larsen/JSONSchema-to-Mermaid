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
private data class SchemaReadContext(val visiting: MutableSet<Path> = LinkedHashSet())

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
                PropertyRequiredFixer.fixRequiredRecursively(prop, propMap) ?: prop
            }
            defSchema.copy(
                properties = fixedProps,
                required = requiredStrings
            )
        }
        val fixedTopLevelProperties = resolved.properties?.mapValues { (propName, prop) ->
            val propMap = (rootMap["properties"] as? Map<*, *>)?.get(propName) as? Map<*, *>
            PropertyRequiredFixer.fixRequiredRecursively(prop, propMap) ?: prop
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
            properties = SchemaMergeUtils.mergeMaps(baseSchema.properties, schema.properties),
            required = SchemaMergeUtils.mergeLists(baseSchema.required, schema.required) ?: emptyList(),
            definitions = SchemaMergeUtils.mergeMaps(baseSchema.definitions, schema.definitions),
            inheritedPropertyNames = inheritedForChild,
        )
    }
}

private fun formatCycleMessage(visiting: Set<Path>, current: Path): String {
    val chain = buildList<Path> {
        addAll(visiting)
        add(current)
    }.joinToString(" -> ") { it.toAbsolutePath().toString() }
    return "Inheritance cycle detected while resolving extends. Chain: $chain"
}

private fun getFileExtension(path: Path): String {
    val fileName = path.fileName.toString()
    val lastDot = fileName.lastIndexOf('.')
    return if (lastDot > 0) fileName.substring(lastDot + 1).lowercase() else ""
}

/**
 * Utility object for merging schema maps and lists.
 */
object SchemaMergeUtils {
    fun <K, V> mergeMaps(base: Map<K, V>?, override: Map<K, V>?): Map<K, V>? =
        when {
            base == null && override == null -> null
            base == null -> override
            override == null -> base
            else -> base + override
        }

    fun <T> mergeLists(base: List<T>?, override: List<T>?): List<T>? =
        when {
            base == null && override == null -> null
            base == null -> override
            override == null -> base
            else -> (base + override).distinct()
        }
}

/**
 * Singleton for recursively fixing required fields in properties and their children.
 */
object PropertyRequiredFixer {
    fun fixRequiredRecursively(
        property: jsonschema_to_mermaid.jsonschema.Property?,
        map: Map<*, *>?
    ): jsonschema_to_mermaid.jsonschema.Property? {
        if (property == null || map == null) return property
        val requiredList = extractRequiredList(map)
        val fixedProperties = fixProperties(property, map)
        val fixedPatternProperties = fixPatternProperties(property, map)
        val fixedItems = fixItems(property, map)
        val fixedAllOf = fixCombinator(property.allOf, map, "allOf")
        val fixedOneOf = fixCombinator(property.oneOf, map, "oneOf")
        val fixedAnyOf = fixCombinator(property.anyOf, map, "anyOf")
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

    private fun extractRequiredList(map: Map<*, *>): List<String> =
        (map["required"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()

    private fun fixProperties(property: jsonschema_to_mermaid.jsonschema.Property, map: Map<*, *>): Map<String, jsonschema_to_mermaid.jsonschema.Property>? =
        property.properties?.mapValues { (k, v) ->
            val propMap = (map["properties"] as? Map<*, *>)?.get(k) as? Map<*, *>
            fixRequiredRecursively(v, propMap)
        }?.filterValues { it != null }?.mapValues { it.value!! }

    private fun fixPatternProperties(property: jsonschema_to_mermaid.jsonschema.Property, map: Map<*, *>): Map<String, jsonschema_to_mermaid.jsonschema.Property>? =
        property.patternProperties?.mapValues { (k, v) ->
            val patMap = (map["patternProperties"] as? Map<*, *>)?.get(k) as? Map<*, *>
            fixRequiredRecursively(v, patMap)
        }?.filterValues { it != null }?.mapValues { it.value!! }

    private fun fixItems(property: jsonschema_to_mermaid.jsonschema.Property, map: Map<*, *>): jsonschema_to_mermaid.jsonschema.Property? =
        property.items?.let {
            val itemsMap = map["items"] as? Map<*, *>
            fixRequiredRecursively(it, itemsMap)
        }

    private fun fixCombinator(
        combinator: List<jsonschema_to_mermaid.jsonschema.Property>?,
        map: Map<*, *>?,
        key: String
    ): List<jsonschema_to_mermaid.jsonschema.Property>? =
        combinator?.mapIndexed { idx, p ->
            val list = map?.get(key) as? List<*>
            val subMap = list?.getOrNull(idx) as? Map<*, *>
            fixRequiredRecursively(p, subMap)
        }?.filterNotNull()
}
