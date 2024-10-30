package com.github.analyzer.models

import com.fasterxml.jackson.annotation.JsonProperty
import kotlinx.serialization.Serializable


@Serializable
data class RepoFile(@JsonProperty("path") val path: String) {
    @JsonProperty("classNames")
    var classNames: List<String>? = emptyList()
    fun getFetchFileQuery(identifier: String): String {
        return """
            file_${identifier}: object(
                expression: "HEAD:${path}"
            ) {
                ... on Blob {
                    text
                }
            }
            """.trimIndent()
    }
}