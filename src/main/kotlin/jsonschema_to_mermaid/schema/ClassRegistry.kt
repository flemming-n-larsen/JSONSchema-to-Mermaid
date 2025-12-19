package jsonschema_to_mermaid.schema

/**
 * Manages class registrations for the diagram.
 */
object ClassRegistry {

    fun ensureClassEntry(classProperties: MutableMap<String, MutableList<String>>, className: String) {
        classProperties.getOrPut(className) { mutableListOf() }
    }
}
