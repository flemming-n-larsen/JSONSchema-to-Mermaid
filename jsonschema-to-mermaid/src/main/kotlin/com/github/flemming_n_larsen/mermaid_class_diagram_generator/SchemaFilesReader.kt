package com.github.flemming_n_larsen.mermaid_class_diagram_generator

import com.google.gson.GsonBuilder
import org.yaml.snakeyaml.Yaml
import java.io.FileReader
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.streams.toList

object SchemaFilesReader {

    private val gson = GsonBuilder().create()
    private val yaml = Yaml()

    fun readSchemas(source: Set<Path>): Collection<Schema> =
        collectAllFiles(source).map { readSchema(it) }

    private fun collectAllFiles(source: Set<Path>): Set<Path> =
        source.flatMap { file ->
            if (file.isDirectory()) {
                collectAllFiles(Files.list(file).toList().toSet())
            } else {
                setOf(file)
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
            val schema = gson.fromJson(fileReader, Schema::class.java)
            if (schema.dollarId.isNullOrBlank()) {
                schema.dollarId = path.fileName.toString()
            }
            return schema
        }
    }

    private fun readYamlSchema(path: Path): Schema {
        FileReader(path.toFile()).use { fileReader ->
            val yaml: Map<String, Any> = yaml.load(fileReader)
            return gson.fromJson(gson.toJson(yaml), Schema::class.java)
        }
    }
}