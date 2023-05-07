package com.github.flemming_n_larsen.mermaid_class_diagram_generator

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.arguments.unique
import com.github.ajalt.clikt.parameters.types.path
import com.google.gson.GsonBuilder
import org.yaml.snakeyaml.Yaml
import java.io.FileReader
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.streams.toList

fun main(args: Array<String>) = GenerateClassDiagrams().main(args)

class GenerateClassDiagrams : CliktCommand() {
    private val source: Set<Path> by argument().path(mustExist = true).multiple().unique()
    private val dest: Path by argument().path()

    private val gson = GsonBuilder().create()
    private val yaml = Yaml()

    override fun run() {

        val schemas = readSchemas()
        schemas.forEach {
            println(it.dollarId)
        }
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

    private fun readJsonSchema(path: Path): Schema =
        gson.fromJson(FileReader(path.toFile()), Schema::class.java)

    private fun readYamlSchema(path: Path): Schema {
        val yaml: Map<String, Any> = yaml.load(FileReader(path.toFile()))
        return gson.fromJson(gson.toJson(yaml),  Schema::class.java)
    }

    private fun readSchemas(): List<Schema> {
        val files = mutableSetOf<Path>()
        collectAllFiles(source, files)

        val schemas = mutableListOf<Schema>()
        files.forEach {
            schemas.add(readSchema(it))
        }
        return schemas
    }
}
