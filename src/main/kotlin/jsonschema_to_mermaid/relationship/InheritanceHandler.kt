package jsonschema_to_mermaid.relationship

import jsonschema_to_mermaid.schema_files.SchemaFileInfo
import jsonschema_to_mermaid.diagram.ClassNameResolver.getClassName
import jsonschema_to_mermaid.diagram.ClassNameResolver.refToClassName

/**
 * Handles schema inheritance relationships.
 */
object InheritanceHandler {

    private var loadedSchemas: List<SchemaFileInfo> = emptyList()
    private var showInheritedFields: Boolean = false

    fun setLoadedSchemas(schemas: List<SchemaFileInfo>, showInheritedFields: Boolean) {
        this.loadedSchemas = schemas
        this.showInheritedFields = showInheritedFields
    }

    fun handleInheritance(
        schemaFile: SchemaFileInfo,
        className: String,
        relations: MutableList<String>
    ) {
        schemaFile.schema.extends?.let { extends ->
            val parentClassName = resolveParentClassName(extends)
            relations.add(RelationshipBuilder.formatInheritanceRelation(parentClassName, className))
        }
    }

    private fun resolveParentClassName(extends: jsonschema_to_mermaid.jsonschema.Extends): String {
        val ref = when (extends) {
            is jsonschema_to_mermaid.jsonschema.Extends.Ref -> extends.ref
            is jsonschema_to_mermaid.jsonschema.Extends.Object -> extends.ref
        }

        return findSchemaByRef(ref)
            ?.let { getClassName(it) }
            ?: refToClassName(ref)
    }

    private fun findSchemaByRef(ref: String?): SchemaFileInfo? = ref?.let { r ->
        loadedSchemas.firstOrNull { schemaFile ->
            matchesSchemaByBaseName(schemaFile, r) || matchesSchemaByTitle(schemaFile, r)
        }
    }

    private fun matchesSchemaByBaseName(schemaFile: SchemaFileInfo, ref: String): Boolean =
        schemaFile.filename
            ?.substringBefore('.')
            ?.let { ref.contains(it) }
            ?: false

    private fun matchesSchemaByTitle(schemaFile: SchemaFileInfo, ref: String): Boolean =
        schemaFile.schema.title?.let { ref.contains(it) } ?: false

    fun shouldSkipInheritedProperty(schemaFile: SchemaFileInfo, propertyName: String): Boolean =
        !showInheritedFields && isInheritedProperty(schemaFile, propertyName)

    fun isInheritedProperty(schemaFile: SchemaFileInfo, propertyName: String): Boolean =
        schemaFile.schema.inheritedPropertyNames
            ?.contains(propertyName)
            ?: false
}
