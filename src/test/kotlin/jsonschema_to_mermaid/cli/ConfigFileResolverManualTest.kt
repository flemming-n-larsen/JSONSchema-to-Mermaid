package jsonschema_to_mermaid.cli

import java.nio.file.Files
import java.nio.file.Paths

fun main() {
    val resolver = ConfigFileResolver()

    // Test 1: Empty file
    val tmpFile1 = Files.createTempFile("test", ".json")
    Files.write(tmpFile1, ByteArray(0))
    try {
        val config1 = resolver.parseConfig(tmpFile1)
        println("Test 1 (empty file): " + "PASS")
    } catch (e: Exception) {
        println("Test 1 (empty file): FAIL - " + e.message)
    }

    // Test 2: Valid JSON with case-insensitive key
    val tmpFile2 = Files.createTempFile("test", ".json")
    Files.write(tmpFile2, "{ \"ArRaYs\": \"inline\" }".toByteArray())
    try {
        val config2 = resolver.parseConfig(tmpFile2)
        val value = resolver.getString(config2, "arrays")
        println("Test 2 (case-insensitive): " + if (value == "inline") "PASS" else "FAIL (got $value)")
    } catch (e: Exception) {
        println("Test 2 (case-insensitive): FAIL - " + e.message)
    }

    // Test 3: Unknown keys
    val tmpFile3 = Files.createTempFile("test", ".json")
    Files.write(tmpFile3, "{ \"arrays\": \"inline\", \"unknownKey\": 123 }".toByteArray())
    try {
        val config3 = resolver.parseConfig(tmpFile3)
        val value = resolver.getString(config3, "arrays")
        println("Test 3 (unknown keys): " + if (value == "inline") "PASS" else "FAIL (got $value)")
    } catch (e: Exception) {
        println("Test 3 (unknown keys): FAIL - " + e.message)
    }
}

