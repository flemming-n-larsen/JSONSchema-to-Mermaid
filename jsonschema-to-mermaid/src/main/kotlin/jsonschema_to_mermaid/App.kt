package jsonschema_to_mermaid

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.arguments.unique
import com.github.ajalt.clikt.parameters.types.path
import java.nio.file.Path

fun main(args: Array<String>) = App.main(args)

object App : CliktCommand() {
//    private val source: Set<Path> by argument().path(mustExist = true).multiple().unique()
//    private val dest: Path by argument().path()

    override fun run() {
        val output = MermaidClassDiagramGenerator.generate(setOf(Path.of("D:\\Code\\JSONSchema-to-Mermaid\\jsonschema-to-mermaid\\src\\test\\resources\\bookstore\\bookstore.schema.json")))
        print(output)
    }
}
