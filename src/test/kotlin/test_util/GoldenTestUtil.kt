package test_util

import java.io.File

object GoldenTestUtil {
    private val goldenDir = File("src/test/resources/golden")
    private val updateGolden = System.getenv("UPDATE_GOLDEN") == "1" || System.getProperty("updateGolden") == "true"

    fun assertMatchesGolden(name: String, actual: String) {
        val goldenFile = File(goldenDir, "$name.mmd")
        if (!goldenFile.exists() || updateGolden) {
            goldenFile.writeText(actual)
            if (!goldenFile.exists()) {
                error("Golden file $name.mmd was created. Please verify and re-run tests.")
            }
        }
        val expected = goldenFile.readText()
        if (actual != expected) {
            error("Output does not match golden file: $name.mmd\n--- Expected ---\n$expected\n--- Actual ---\n$actual")
        }
    }
}

