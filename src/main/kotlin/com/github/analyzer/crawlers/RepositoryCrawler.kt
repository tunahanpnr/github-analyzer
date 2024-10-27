package com.github.analyzer.crawlers

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable


interface RepositoryCrawler {
    @Serializable
    data class PrimaryLanguage(val name: String?)

    @Serializable
    data class RepositoryInfo(val name: String, val url: String, val primaryLanguage: PrimaryLanguage?)

    @Serializable
    data class Search(val nodes: List<RepositoryInfo>)

    @Serializable
    data class Data(val search: Search)

    @Serializable
    data class SearchRepoResponse(val data: Data)

    val BASE_URL: String
        get() = "https://api.github.com/graphql"
    val GRAPHQL_QUERY: String
        get() = """
                    query {
                        search(query: "$LANGUAGE", type: REPOSITORY, first: $LIMIT) {
                            nodes {
                                ... on Repository {
                                    name
                                    url
                                    primaryLanguage {
                                        name
                                    }
                                }
                            }
                        }
                    }
                """
    val GITHUB_TOKEN: String
        get() = System.getenv("GITHUB_TOKEN")
    val LANGUAGE: String
    val LIMIT: String
    val client: HttpClient
        get() = HttpClient(CIO) {
            install(Auth) {
                bearer {
                    loadTokens {
                        BearerTokens(GITHUB_TOKEN, "")
                    }
                }
            }
            install(ContentNegotiation) {
                json()
            }
        }

    suspend fun searchRepositories(): List<RepositoryInfo> {
        val response = client.post(BASE_URL) {
            contentType(ContentType.Application.Json)
            setBody(mapOf("query" to GRAPHQL_QUERY))
        }.body<SearchRepoResponse>()
        return response.data.search.nodes
    }
}
