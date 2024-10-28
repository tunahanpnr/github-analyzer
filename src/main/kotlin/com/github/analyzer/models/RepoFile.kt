package com.github.analyzer.models

import kotlinx.serialization.Serializable


@Serializable
data class RepoFile(val path: String) {
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