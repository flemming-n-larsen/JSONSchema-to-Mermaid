package com.github.flemming_n_larsen.mermaid_class_diagram_generator

import com.google.gson.annotations.SerializedName

data class Schema(
    @SerializedName("\$id")
    var dollarId: String?,

    @SerializedName("\$schema")
    var dollarSchema: String?,
)
