package jsonschema_to_mermaid.schema_files

import jsonschema_to_mermaid.jsonschema.Schema

data class SchemaFileInfo(
    val filename: String? = null,
    val schema: Schema,
)