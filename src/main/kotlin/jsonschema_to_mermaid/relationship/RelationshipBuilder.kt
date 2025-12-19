package jsonschema_to_mermaid.relationship

/**
 * Responsible for building Mermaid relation strings.
 */
object RelationshipBuilder {

    fun formatRelation(
        fromClassName: String,
        toClassName: String,
        fromMultiplicity: String? = null,
        toMultiplicity: String? = null,
        label: String,
        arrow: String = "-->"
    ): String = buildString {
        append(fromClassName)
        append(" ")
        fromMultiplicity?.let { append("\"$it\" ") }
        append(arrow)
        toMultiplicity?.let { append(" \"$it\"") }
        append(" $toClassName : $label")
    }

    fun formatInheritanceRelation(parentClassName: String, childClassName: String): String =
        "$parentClassName <|-- $childClassName"
}
