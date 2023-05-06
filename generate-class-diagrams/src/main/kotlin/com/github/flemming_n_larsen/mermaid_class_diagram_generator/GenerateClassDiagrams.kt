package com.github.flemming_n_larsen.mermaid_class_diagram_generator

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.*
import com.github.ajalt.clikt.parameters.types.*
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.streams.toList

fun main(args: Array<String>) = GenerateClassDiagrams().main(args)

class GenerateClassDiagrams : CliktCommand() {
    private val source: Set<Path> by argument().path(mustExist = true).multiple().unique()
    private val dest: Path by argument().path()

    override fun run() {

        println(source)

        val files = HashSet<Path>()
        collectAllFiles(source, files)

        files.forEach {
            println(it)
        }
    }

    private fun collectAllFiles(source: Set<Path>, fileSetOut: MutableSet<Path>) {
        source.forEach { file ->
            if (file.isDirectory()) {
                collectAllFiles(Files.list(file) .toList().toSet(), fileSetOut)
            } else {
                fileSetOut.add(file)
            }
        }
    }
}
