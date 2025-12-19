package test_util

import java.io.File

object GoldenTestUtil {
    private val goldenDir = File("src/test/resources/golden")
    private val updateGolden = System.getenv("UPDATE_GOLDEN") == "1" || System.getProperty("updateGolden") == "true"

    private fun normalize(text: String): String = text
        .replace("\r\n", "\n")
        .replace("\r", "\n")
        .lines()
        .joinToString("\n") { it.trimEnd() }
        .trim()

    fun assertMatchesGolden(name: String, actual: String) {
        val goldenFile = File(goldenDir, "$name.mmd")

        if (!goldenFile.exists() || updateGolden) {
            goldenFile.writeText(actual)
            check(goldenFile.exists()) { "Golden file $name.mmd was created. Please verify and re-run tests." }
        }

        val normExpected = normalize(goldenFile.readText())
        val normActual = normalize(actual)

        check(normActual == normExpected) {
            "Output does not match golden file: $name.mmd\n--- Expected ---\n$normExpected\n--- Actual ---\n$normActual"
        }
    }
}
