package com.github.flemming_n_larsen.mermaid_class_diagram_generator

import com.google.gson.GsonBuilder
import org.yaml.snakeyaml.Yaml
import java.io.FileReader
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.streams.toList

object SchemaFilesReader {

    private val gson = GsonBuilder().create()
    private val yaml = Yaml()

    fun readSchemas(source: Set<Path>): Collection<Schema> {
        val files = mutableSetOf<Path>()
        collectAllFiles(source, files)

        val schemas = mutableListOf<Schema>()
        files.forEach {
            schemas.add(readSchema(it))
        }
        return schemas
    }

    private fun collectAllFiles(source: Set<Path>, fileSetOut: MutableSet<Path>) {
        source.forEach { file ->
            if (file.isDirectory()) {
                collectAllFiles(Files.list(file).toList().toSet(), fileSetOut)
            } else {
                fileSetOut.add(file)
            }
        }
    }

    private fun readSchema(path: Path): Schema =
        when (getFileExtension(path)) {
            "json" -> readJsonSchema(path)
            "yaml", "yml" -> readYamlSchema(path)
            else -> throw IOException("Unsupported file type: " + path.fileName.toString())
        }

    private fun getFileExtension(path: Path): String {
        val fileName = path.fileName.toString()
        val lastIndexOf = fileName.lastIndexOf(".")
        return if (lastIndexOf > 0) fileName.substring(lastIndexOf + 1).lowercase() else ""
    }

    private fun readJsonSchema(path: Path): Schema {
        val schema = gson.fromJson(FileReader(path.toFile()), Schema::class.java)
        if (schema.dollarId.isNullOrBlank()) {
            schema.dollarId = path.fileName.toString()
        }
        return schema
    }

    private fun readYamlSchema(path: Path): Schema {
        val yaml: Map<String, Any> = yaml.load(FileReader(path.toFile()))
        return gson.fromJson(gson.toJson(yaml), Schema::class.java)
    }
}