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
        loadedSchemas = schemas
        this.showInheritedFields = showInheritedFields
    }

    fun handleInheritance(
        schemaFile: SchemaFileInfo,
        className: String,
        relations: MutableList<String>
    ) {
        val extends = schemaFile.schema.extends ?: return
        
        val parentClassName = resolveParentClassName(extends)
        val relation = RelationshipBuilder.formatInheritanceRelation(parentClassName, className)
        relations.add(relation)
    }

    private fun resolveParentClassName(extends: jsonschema_to_mermaid.jsonschema.Extends): String {
        val ref = when (extends) {
            is jsonschema_to_mermaid.jsonschema.Extends.Ref -> extends.ref
            is jsonschema_to_mermaid.jsonschema.Extends.Object -> extends.ref
        }
        
        val parentSchemaFile = findSchemaByRef(ref)
        return if (parentSchemaFile != null) {
            getClassName(parentSchemaFile)
        } else {
            refToClassName(ref)
        }
    }

    private fun findSchemaByRef(ref: String?): SchemaFileInfo? {
        if (ref == null) return null
        
        return loadedSchemas.firstOrNull { schemaFile ->
            matchesSchemaByBaseName(schemaFile, ref) || matchesSchemaByTitle(schemaFile, ref)
        }
    }

    private fun matchesSchemaByBaseName(schemaFile: SchemaFileInfo, ref: String): Boolean {
        val baseName = schemaFile.filename?.substringBefore('.')
        return baseName != null && ref.contains(baseName)
    }

    private fun matchesSchemaByTitle(schemaFile: SchemaFileInfo, ref: String): Boolean {
        return schemaFile.schema.title != null && ref.contains(schemaFile.schema.title)
    }

    fun shouldSkipInheritedProperty(schemaFile: SchemaFileInfo, propertyName: String): Boolean {
        if (showInheritedFields) return false
        return isInheritedProperty(schemaFile, propertyName)
    }

    fun isInheritedProperty(schemaFile: SchemaFileInfo, propertyName: String): Boolean {
        val inheritedSet = schemaFile.schema.inheritedPropertyNames?.toSet() ?: emptySet()
        return inheritedSet.contains(propertyName)
    }
}
