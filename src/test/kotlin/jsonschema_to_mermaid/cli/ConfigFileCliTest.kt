package jsonschema_to_mermaid.cli

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import test_util.resourcePath
import java.nio.file.Files
import java.nio.file.StandardCopyOption

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
})
