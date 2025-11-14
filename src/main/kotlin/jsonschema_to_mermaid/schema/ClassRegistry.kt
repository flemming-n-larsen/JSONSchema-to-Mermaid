package jsonschema_to_mermaid.schema

/**
 * Manages class registrations for the diagram.
 */
object ClassRegistry {
    
    fun ensureClassEntry(classProperties: MutableMap<String, MutableList<String>>, className: String) {
        if (!classProperties.containsKey(className)) {
            classProperties[className] = mutableListOf()
        }
    }
}
