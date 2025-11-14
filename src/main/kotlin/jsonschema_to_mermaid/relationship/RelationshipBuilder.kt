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
    ): String {
        val fromPart = if (fromMultiplicity != null) "\"$fromMultiplicity\" " else ""
        val toPart = if (toMultiplicity != null) " \"$toMultiplicity\"" else ""
        return "$fromClassName $fromPart$arrow$toPart $toClassName : $label"
    }

    fun formatInheritanceRelation(parentClassName: String, childClassName: String): String {
        return "$parentClassName <|-- $childClassName"
    }
}
