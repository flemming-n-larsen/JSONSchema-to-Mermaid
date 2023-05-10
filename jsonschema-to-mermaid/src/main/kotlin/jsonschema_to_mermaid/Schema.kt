package jsonschema_to_mermaid

import com.google.gson.annotations.SerializedName

data class Schema(
    @SerializedName("\$id")
    var dollarId: String?,

    @SerializedName("\$schema")
    var dollarSchema: String?,

    var properties: Map<String, Any>?
)