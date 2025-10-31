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

    fun readSchemas(sourcePaths: Set<Path>): List<SchemaFileInfo> {
        return collectAllFiles(sourcePaths).map { filepath ->
            val schema = readSchema(filepath)
            SchemaFileInfo(filepath.name, schema)
        }
    }

    private fun collectAllFiles(sourcePaths: Set<Path>): Set<Path> =
        sourcePaths.flatMap { file ->
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
        if (!abs.toFile().exists()) throw FileFormatException("Schema file not found: $abs")
        if (!visiting.add(abs)) {
            throw InheritanceCycleException(formatCycleMessage(visiting, abs))
        }
        val (schema, rootMap) = when (getFileExtension(abs)) {
            "json" -> parseJsonSchemaWithMap(abs)
            "yaml", "yml" -> parseYamlSchemaWithMap(abs)
            else -> throw FileFormatException("Unsupported schema file type 'abs.fileName}' at path: $abs")
        }
        val resolved = resolveExtends(schema, abs, visiting)
        visiting.remove(abs)
        // Post-process definitions & top-level properties to ensure required fields are set from the original map (recursively)
        fun fixRequiredRecursively(
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
        // Fix definitions (each definition schema and its nested properties)
        val fixedDefinitions = resolved.definitions?.mapValues { (defName, defSchema) ->
            val defMap = (rootMap["definitions"] as? Map<*, *>)?.get(defName) as? Map<*, *>
            val requiredListAny = defMap?.entries?.firstOrNull { (k, _) -> k.toString() == "required" }?.value as? List<*> ?: emptyList<Any>()
            val requiredStrings = requiredListAny.filterIsInstance<String>()
            val fixedProps = defSchema.properties?.mapValues { (propName, prop) ->
                val propMap = (defMap?.get("properties") as? Map<*, *>)?.get(propName) as? Map<*, *>
                fixRequiredRecursively(prop, propMap) ?: prop
            }
            defSchema.copy(
                properties = fixedProps,
                required = requiredStrings
            )
        }
        // Fix top-level properties recursively
        val fixedTopLevelProperties = resolved.properties?.mapValues { (propName, prop) ->
            val propMap = (rootMap["properties"] as? Map<*, *>)?.get(propName) as? Map<*, *>
            fixRequiredRecursively(prop, propMap) ?: prop
        }
        // Return new Schema with updated definitions & top-level properties; keep merged required list from inheritance resolution
        return resolved.copy(definitions = fixedDefinitions, properties = fixedTopLevelProperties)
    }

    // Parse JSON schema and return both the Schema object and the root map
    private fun parseJsonSchemaWithMap(path: Path): Pair<Schema, Map<String, Any>> {
        return try {
            FileReader(path.toFile()).use { fileReader ->
                val gsonMap: Map<String, Any> = gson.fromJson(fileReader, Map::class.java) as Map<String, Any>
                val schema = gson.fromJson(gson.toJson(gsonMap), Schema::class.java)
                    ?: throw FileFormatException("Could not parse JSON schema file: null result")
                schema to gsonMap
            }
        } catch (e: Exception) {
            throw FileFormatException("Could not parse JSON schema file", e)
        }
    }

    // Parse YAML schema and return both the Schema object and the root map
    private fun parseYamlSchemaWithMap(path: Path): Pair<Schema, Map<String, Any>> {
        return try {
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
    }

    // Updated resolveExtends using shared visited set
    private fun resolveExtends(schema: Schema, path: Path, visiting: MutableSet<Path>): Schema {
        val extends = schema.extends ?: return schema
        val refPath = when (extends) {
            is Extends.Ref -> path.parent.resolve(extends.ref)
            is Extends.Object -> path.parent.resolve(extends.ref)
        }.toAbsolutePath().normalize()
        // Recursive call will handle cycle detection
        val baseSchema = readSchema(refPath, visiting)
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

    private fun formatCycleMessage(visiting: Set<Path>, current: Path): String {
        // visiting already contains current path (since add failed). Build ordered chain including current again at end.
        val chain = (visiting.toList() + current).joinToString(" -> ") { it.toAbsolutePath().toString() }
        return "Inheritance cycle detected while resolving extends. Chain: $chain"
    }

    private fun getFileExtension(path: Path): String {
        val fileName = path.fileName.toString()
        val lastDot = fileName.lastIndexOf('.')
        return if (lastDot > 0) fileName.substring(lastDot + 1).lowercase() else ""
    }
}
