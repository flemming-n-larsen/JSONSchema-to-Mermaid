@file:JvmName("TestAllOfModesKt")
package test_manual

import jsonschema_to_mermaid.schema_files.SchemaFilesReader
import jsonschema_to_mermaid.diagram.MermaidGenerator
import jsonschema_to_mermaid.diagram.Preferences
import jsonschema_to_mermaid.diagram.AllOfMode
import java.nio.file.Paths

fun main() {
    println("Testing AllOf Mode Implementation")
    println("=".repeat(50))
    println()

    // Test merge mode
    val mergePath = Paths.get("src/test/resources/core/allof_merge.schema.json")
    val mergeSchemas = SchemaFilesReader.readSchemas(setOf(mergePath))
    val mergeDiagram = MermaidGenerator.generate(mergeSchemas, preferences = Preferences(allOfMode = AllOfMode.MERGE))
    println("=== MERGE MODE (default) ===")
    println(mergeDiagram)
    println()

    // Test inherit mode
    val inheritPath = Paths.get("src/test/resources/core/allof_inherit.schema.json")
    val inheritSchemas = SchemaFilesReader.readSchemas(setOf(inheritPath))
    val inheritDiagram = MermaidGenerator.generate(inheritSchemas, preferences = Preferences(allOfMode = AllOfMode.INHERIT))
    println("=== INHERIT MODE ===")
    println(inheritDiagram)
    println()

    // Test compose mode
    val composePath = Paths.get("src/test/resources/core/allof_compose.schema.json")
    val composeSchemas = SchemaFilesReader.readSchemas(setOf(composePath))
    val composeDiagram = MermaidGenerator.generate(composeSchemas, preferences = Preferences(allOfMode = AllOfMode.COMPOSE))
    println("=== COMPOSE MODE ===")
    println(composeDiagram)

    println()
    println("=".repeat(50))
    println("✓ All modes executed successfully!")
}

