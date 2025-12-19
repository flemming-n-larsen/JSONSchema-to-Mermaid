package jsonschema_to_mermaid.diagram

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
    ): String = buildString {
        appendHeader(noClassDiagramHeader)
        appendClasses(classProperties)
        appendEnums(preferences, enumNotes, enumClasses)
        appendRelations(relations)
    }.trimEnd() + "\n"

    private fun StringBuilder.appendHeader(noClassDiagramHeader: Boolean) {
        if (!noClassDiagramHeader) appendLine("classDiagram")
    }

    private fun StringBuilder.appendClasses(classProperties: Map<String, List<String>>) {
        classProperties.forEach { (className, properties) ->
            appendLine("  class $className {")
            properties.forEach { property -> appendLine("    $property") }
            appendLine("  }")
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
            EnumStyle.INLINE -> Unit // No additional output needed
        }
    }

    private fun StringBuilder.appendEnumNotes(enumNotes: List<Pair<String, String>>) {
        enumNotes.forEach { (className, note) ->
            appendLine("  note for $className \"$note\"")
        }
    }

    private fun StringBuilder.appendEnumClasses(enumClasses: List<Pair<String, List<String>>>) {
        enumClasses.forEach { (enumName, values) ->
            appendLine("  class $enumName {")
            values.forEach { value -> appendLine("    $value") }
            appendLine("  }")
            appendLine("  <<enumeration>> $enumName")
        }
    }

    private fun StringBuilder.appendRelations(relations: List<String>) {
        if (relations.isNotEmpty()) {
            appendLine()
            relations.forEach { relation -> appendLine("  $relation") }
        }
    }
}
