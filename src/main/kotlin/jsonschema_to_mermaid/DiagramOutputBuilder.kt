package jsonschema_to_mermaid

/**
 * Builds the final Mermaid diagram output from processed classes and relationships.
 */
object DiagramOutputBuilder {

    fun buildOutput(
        classProperties: Map<String, List<String>>,
        relations: List<String>,
        noClassDiagramHeader: Boolean,
        preferences: Preferences,
        enumNotes: List<Pair<String, String>>,
        enumClasses: List<Pair<String, List<String>>>
    ): String {
        return StringBuilder().apply {
            appendHeader(noClassDiagramHeader)
            appendClasses(classProperties)
            appendEnums(preferences, enumNotes, enumClasses)
            appendRelations(relations)
        }.toString()
    }

    private fun StringBuilder.appendHeader(noClassDiagramHeader: Boolean) {
        if (!noClassDiagramHeader) {
            append("classDiagram\n")
        }
    }

    private fun StringBuilder.appendClasses(classProperties: Map<String, List<String>>) {
        classProperties.forEach { (className, properties) ->
            append("  class $className {\n")
            properties.forEach { property ->
                append("    $property\n")
            }
            append("  }\n")
        }
    }

    private fun StringBuilder.appendEnums(
        preferences: Preferences,
        enumNotes: List<Pair<String, String>>,
        enumClasses: List<Pair<String, List<String>>>
    ) {
        when (preferences.enumStyle) {
            EnumStyle.NOTE -> appendEnumNotes(enumNotes)
            EnumStyle.CLASS -> appendEnumClasses(enumClasses)
            EnumStyle.INLINE -> {} // No additional output needed
        }
    }

    private fun StringBuilder.appendEnumNotes(enumNotes: List<Pair<String, String>>) {
        enumNotes.forEach { (className, note) ->
            append("  note for $className \"$note\"\n")
        }
    }

    private fun StringBuilder.appendEnumClasses(enumClasses: List<Pair<String, List<String>>>) {
        enumClasses.forEach { (enumName, values) ->
            append("  class $enumName {\n")
            values.forEach { value ->
                append("    $value\n")
            }
            append("  }\n  <<enumeration>> $enumName\n")
        }
    }

    private fun StringBuilder.appendRelations(relations: List<String>) {
        if (relations.isNotEmpty()) {
            append('\n')
            relations.forEach { relation ->
                append("  $relation\n")
            }
        }
    }
}

