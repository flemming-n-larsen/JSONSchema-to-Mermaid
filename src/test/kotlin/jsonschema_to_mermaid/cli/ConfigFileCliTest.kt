package jsonschema_to_mermaid.cli

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import test_util.resourcePath
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.Paths

@Suppress("unused")
class ConfigFileCliTest : FunSpec({
    test("project-level js2m.json sets arrays inline when no CLI flag") {
        val tmpDir = Files.createTempDirectory("js2m-test-")
        try {
            val schemaSrc = resourcePath("/readme_examples/order.schema.json")
            Files.copy(schemaSrc, tmpDir.resolve("order.schema.json"), StandardCopyOption.REPLACE_EXISTING)
            val config = "{ \"arrays\": \"inline\" }"
            Files.write(tmpDir.resolve("js2m.json"), config.toByteArray())

            val outSb = StringBuilder()
            val errSb = StringBuilder()
            val echo: (String, Boolean) -> Unit = { msg, isErr -> if (isErr) errSb.append(msg).append('\n') else outSb.append(msg).append('\n') }
            val options = CliOptions(sourceDirOption = tmpDir)
            val svc = CliService(options, echo)
            val originalOut = System.out
            val originalErr = System.err
            try {
                val baosOut = java.io.ByteArrayOutputStream()
                val baosErr = java.io.ByteArrayOutputStream()
                System.setOut(java.io.PrintStream(baosOut))
                System.setErr(java.io.PrintStream(baosErr))
                svc.execute()
                outSb.append(String(baosOut.toByteArray()))
                errSb.append(String(baosErr.toByteArray()))
            } finally {
                System.setOut(originalOut)
                System.setErr(originalErr)
            }

            val out = outSb.toString()
            out.shouldNotContain("Order \"1\" --> \"*\" OrderItem")
            out.shouldContain("Object[] Item")
        } finally {
            tmpDir.toFile().deleteRecursively()
        }
    }

    test("explicit flag overrides project config") {
        val tmpDir = Files.createTempDirectory("js2m-test-")
        try {
            val schemaSrc = resourcePath("/readme_examples/order.schema.json")
            Files.copy(schemaSrc, tmpDir.resolve("order.schema.json"), StandardCopyOption.REPLACE_EXISTING)
            val config = "{ \"arrays\": \"relation\" }"
            Files.write(tmpDir.resolve("js2m.json"), config.toByteArray())

            val outSb = StringBuilder()
            val errSb = StringBuilder()
            val echo: (String, Boolean) -> Unit = { msg, isErr -> if (isErr) errSb.append(msg).append('\n') else outSb.append(msg).append('\n') }
            val options = CliOptions(sourceDirOption = tmpDir, arraysOption = "inline")
            val svc = CliService(options, echo)
            val originalOut = System.out
            val originalErr = System.err
            try {
                val baosOut = java.io.ByteArrayOutputStream()
                val baosErr = java.io.ByteArrayOutputStream()
                System.setOut(java.io.PrintStream(baosOut))
                System.setErr(java.io.PrintStream(baosErr))
                svc.execute()
                outSb.append(String(baosOut.toByteArray()))
                errSb.append(String(baosErr.toByteArray()))
            } finally {
                System.setOut(originalOut)
                System.setErr(originalErr)
            }

            val out = outSb.toString()
            out.shouldNotContain("Order \"1\" --> \"*\" OrderItem")
            out.shouldContain("Object[] Item")
        } finally {
            tmpDir.toFile().deleteRecursively()
        }
    }

    test("invalid JSON in config results in stderr message") {
        val tmpDir = Files.createTempDirectory("js2m-test-")
        try {
            val schemaSrc = resourcePath("/readme_examples/order.schema.json")
            Files.copy(schemaSrc, tmpDir.resolve("order.schema.json"), StandardCopyOption.REPLACE_EXISTING)
            val config = "{ invalid json "
            Files.write(tmpDir.resolve("js2m.json"), config.toByteArray())

            val outSb = StringBuilder()
            val errSb = StringBuilder()
            val echo: (String, Boolean) -> Unit = { msg, isErr -> if (isErr) errSb.append(msg).append('\n') else outSb.append(msg).append('\n') }
            val options = CliOptions(sourceDirOption = tmpDir)
            val svc = CliService(options, echo)
            val originalOut = System.out
            val originalErr = System.err
            try {
                val baosOut = java.io.ByteArrayOutputStream()
                val baosErr = java.io.ByteArrayOutputStream()
                System.setOut(java.io.PrintStream(baosOut))
                System.setErr(java.io.PrintStream(baosErr))
                svc.execute()
                outSb.append(String(baosOut.toByteArray()))
                errSb.append(String(baosErr.toByteArray()))
            } finally {
                System.setOut(originalOut)
                System.setErr(originalErr)
            }

            val err = errSb.toString()
            val combined = err + outSb.toString()
            combined.shouldContain("Invalid JSON in config file")
        } finally {
            tmpDir.toFile().deleteRecursively()
        }
    }

    test("empty config file uses defaults and does not crash") {
        val tmpDir = Files.createTempDirectory("js2m-test-")
        try {
            val schemaSrc = resourcePath("/readme_examples/order.schema.json")
            Files.copy(schemaSrc, tmpDir.resolve("order.schema.json"), StandardCopyOption.REPLACE_EXISTING)
            Files.write(tmpDir.resolve("js2m.json"), ByteArray(0))

            val outSb = StringBuilder()
            val errSb = StringBuilder()
            val echo: (String, Boolean) -> Unit = { msg, isErr -> if (isErr) errSb.append(msg).append('\n') else outSb.append(msg).append('\n') }
            val options = CliOptions(sourceDirOption = tmpDir)
            val svc = CliService(options, echo)
            val originalOut = System.out
            val originalErr = System.err
            try {
                val baosOut = java.io.ByteArrayOutputStream()
                val baosErr = java.io.ByteArrayOutputStream()
                System.setOut(java.io.PrintStream(baosOut))
                System.setErr(java.io.PrintStream(baosErr))
                svc.execute()
                outSb.append(String(baosOut.toByteArray()))
                errSb.append(String(baosErr.toByteArray()))
            } finally {
                System.setOut(originalOut)
                System.setErr(originalErr)
            }
            val out = outSb.toString()
            out.shouldContain("Order \"1\" --> \"*\" OrderItem") // default is relation
        } finally {
            tmpDir.toFile().deleteRecursively()
        }
    }

    test("unknown keys in config are ignored and do not crash") {
        val tmpDir = Files.createTempDirectory("js2m-test-")
        try {
            val schemaSrc = resourcePath("/readme_examples/order.schema.json")
            Files.copy(schemaSrc, tmpDir.resolve("order.schema.json"), StandardCopyOption.REPLACE_EXISTING)
            val config = "{ \"arrays\": \"inline\", \"unknownKey\": 123 }"
            Files.write(tmpDir.resolve("js2m.json"), config.toByteArray())
            val outSb = StringBuilder()
            val errSb = StringBuilder()
            val echo: (String, Boolean) -> Unit = { msg, isErr -> if (isErr) errSb.append(msg).append('\n') else outSb.append(msg).append('\n') }
            val options = CliOptions(sourceDirOption = tmpDir)
            val svc = CliService(options, echo)
            val originalOut = System.out
            val originalErr = System.err
            try {
                val baosOut = java.io.ByteArrayOutputStream()
                val baosErr = java.io.ByteArrayOutputStream()
                System.setOut(java.io.PrintStream(baosOut))
                System.setErr(java.io.PrintStream(baosErr))
                svc.execute()
                outSb.append(String(baosOut.toByteArray()))
                errSb.append(String(baosErr.toByteArray()))
            } finally {
                System.setOut(originalOut)
                System.setErr(originalErr)
            }
            val out = outSb.toString()
            out.shouldContain("Object[] Item") // arrays inline
        } finally {
            tmpDir.toFile().deleteRecursively()
        }
    }

    test("mixed-case keys in config are handled case-insensitively") {
        val tmpDir = Files.createTempDirectory("js2m-test-")
        try {
            val schemaSrc = resourcePath("/readme_examples/order.schema.json")
            Files.copy(schemaSrc, tmpDir.resolve("order.schema.json"), StandardCopyOption.REPLACE_EXISTING)
            val config = "{ \"ArRaYs\": \"inline\" }"
            Files.write(tmpDir.resolve("js2m.json"), config.toByteArray())
            val outSb = StringBuilder()
            val errSb = StringBuilder()
            val echo: (String, Boolean) -> Unit = { msg, isErr -> if (isErr) errSb.append(msg).append('\n') else outSb.append(msg).append('\n') }
            val options = CliOptions(sourceDirOption = tmpDir)
            val svc = CliService(options, echo)
            val originalOut = System.out
            val originalErr = System.err
            try {
                val baosOut = java.io.ByteArrayOutputStream()
                val baosErr = java.io.ByteArrayOutputStream()
                System.setOut(java.io.PrintStream(baosOut))
                System.setErr(java.io.PrintStream(baosErr))
                svc.execute()
                outSb.append(String(baosOut.toByteArray()))
                errSb.append(String(baosErr.toByteArray()))
            } finally {
                System.setOut(originalOut)
                System.setErr(originalErr)
            }
            val out = outSb.toString()
            out.shouldContain("Object[] Item") // arrays inline
        } finally {
            tmpDir.toFile().deleteRecursively()
        }
    }

    test("user-level config is used if no project or repo config exists") {
        val tmpDir = Files.createTempDirectory("js2m-test-")
        val homeConfig = Paths.get(System.getProperty("user.home")).resolve(".js2m.json")
        val backup = if (Files.exists(homeConfig)) Files.readAllBytes(homeConfig) else null
        try {
            val schemaSrc = resourcePath("/readme_examples/order.schema.json")
            Files.copy(schemaSrc, tmpDir.resolve("order.schema.json"), StandardCopyOption.REPLACE_EXISTING)
            val config = "{ \"arrays\": \"inline\" }"
            Files.write(homeConfig, config.toByteArray())
            val outSb = StringBuilder()
            val errSb = StringBuilder()
            val echo: (String, Boolean) -> Unit = { msg, isErr -> if (isErr) errSb.append(msg).append('\n') else outSb.append(msg).append('\n') }
            val options = CliOptions(sourceDirOption = tmpDir)
            val svc = CliService(options, echo)
            val originalOut = System.out
            val originalErr = System.err
            try {
                val baosOut = java.io.ByteArrayOutputStream()
                val baosErr = java.io.ByteArrayOutputStream()
                System.setOut(java.io.PrintStream(baosOut))
                System.setErr(java.io.PrintStream(baosErr))
                svc.execute()
                outSb.append(String(baosOut.toByteArray()))
                errSb.append(String(baosErr.toByteArray()))
            } finally {
                System.setOut(originalOut)
                System.setErr(originalErr)
            }
            val out = outSb.toString()
            out.shouldContain("Object[] Item") // arrays inline
        } finally {
            if (backup != null) Files.write(homeConfig, backup) else Files.deleteIfExists(homeConfig)
            tmpDir.toFile().deleteRecursively()
        }
    }

    test("missing config uses defaults") {
        val tmpDir = Files.createTempDirectory("js2m-test-")
        try {
            val schemaSrc = resourcePath("/readme_examples/order.schema.json")
            Files.copy(schemaSrc, tmpDir.resolve("order.schema.json"), StandardCopyOption.REPLACE_EXISTING)
            val outSb = StringBuilder()
            val errSb = StringBuilder()
            val echo: (String, Boolean) -> Unit = { msg, isErr -> if (isErr) errSb.append(msg).append('\n') else outSb.append(msg).append('\n') }
            val options = CliOptions(sourceDirOption = tmpDir)
            val svc = CliService(options, echo)
            val originalOut = System.out
            val originalErr = System.err
            try {
                val baosOut = java.io.ByteArrayOutputStream()
                val baosErr = java.io.ByteArrayOutputStream()
                System.setOut(java.io.PrintStream(baosOut))
                System.setErr(java.io.PrintStream(baosErr))
                svc.execute()
                outSb.append(String(baosOut.toByteArray()))
                errSb.append(String(baosErr.toByteArray()))
            } finally {
                System.setOut(originalOut)
                System.setErr(originalErr)
            }
            val out = outSb.toString()
            out.shouldContain("Order \"1\" --> \"*\" OrderItem") // default is relation
        } finally {
            tmpDir.toFile().deleteRecursively()
        }
    }

    test("repo-level config in parent directory is used if present") {
        val parentDir = Files.createTempDirectory("js2m-parent-")
        val childDir = Files.createTempDirectory(parentDir, "js2m-child-")
        try {
            val schemaSrc = resourcePath("/readme_examples/order.schema.json")
            Files.copy(schemaSrc, childDir.resolve("order.schema.json"), StandardCopyOption.REPLACE_EXISTING)
            val config = "{ \"arrays\": \"inline\" }"
            Files.write(parentDir.resolve("js2m.json"), config.toByteArray())
            val outSb = StringBuilder()
            val errSb = StringBuilder()
            val echo: (String, Boolean) -> Unit = { msg, isErr -> if (isErr) errSb.append(msg).append('\n') else outSb.append(msg).append('\n') }
            val options = CliOptions(sourceDirOption = childDir)
            val svc = CliService(options, echo)
            val originalOut = System.out
            val originalErr = System.err
            try {
                val baosOut = java.io.ByteArrayOutputStream()
                val baosErr = java.io.ByteArrayOutputStream()
                System.setOut(java.io.PrintStream(baosOut))
                System.setErr(java.io.PrintStream(baosErr))
                svc.execute()
                outSb.append(String(baosOut.toByteArray()))
                errSb.append(String(baosErr.toByteArray()))
            } finally {
                System.setOut(originalOut)
                System.setErr(originalErr)
            }
            val out = outSb.toString()
            out.shouldContain("Object[] Item") // arrays inline
        } finally {
            parentDir.toFile().deleteRecursively()
        }
    }
})
