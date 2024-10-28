package com.github.analyzer.crawlers

import com.github.analyzer.models.RepoFile
import com.github.analyzer.models.Repository
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json


interface RepositoryCrawler {
    @Serializable
    data class Search(val nodes: List<Repository>)

    @Serializable
    data class SearchRepoData(val search: Search)

    @Serializable
    data class SearchRepoResponse(val data: SearchRepoData)

    @Serializable
    data class FetchRepoFilesResponse(val tree: List<RepoFile>)

    @Serializable
    data class FetchContentResponse(val data: FetchContentData)

    @Serializable
    data class FetchContentData(val repository: Map<String, FetchContentText>)

    @Serializable
    data class FetchContentText(val text: String)


    val LANGUAGE: String
    val FILE_EXTENSION: String
    val LIMIT: String

    val GRAPHQL_URL: String
        get() = "https://api.github.com/graphql"
    val REPOSITORY_QUERY: String
        get() = """
                    query {
                        search(query: "language:$LANGUAGE", type: REPOSITORY, first: $LIMIT) {
                            nodes {
                                ... on Repository {
                                    name
                                    url
                                    primaryLanguage {
                                        name
                                    }
                                    branch: defaultBranchRef {
                                        name
                                    }
                                    owner {
                                        username: login
                                    }
                                }
                            }
                        }
                    }
                """
    val GITHUB_TOKEN: String
        get() = System.getenv("GITHUB_TOKEN")

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
                json(Json { ignoreUnknownKeys = true })
            }
        }

    suspend fun fetchRepositories(): List<Repository> {
        val response = client.post(GRAPHQL_URL) {
            contentType(ContentType.Application.Json)
            setBody(mapOf("query" to REPOSITORY_QUERY))
        }.body<SearchRepoResponse>()

        return response.data.search.nodes
    }

    suspend fun fetchRepositoryFiles(repository: Repository): List<RepoFile> {
        try {
            val response =
                client.get("https://api.github.com/repos/${repository.owner.username}/${repository.name}/git/trees/${repository.branch.name}?recursive=1") {
                    contentType(ContentType.Application.Json)
                }.body<FetchRepoFilesResponse>()
            return response.tree.filter { it.path.removeSurrounding("\"").endsWith(FILE_EXTENSION) }
        } catch (e: HttpRequestTimeoutException) {
            println("Request timeout: \"https://api.github.com/repos/${repository.owner.username}/${repository.name}/git/trees/${repository.branch.name}?recursive=1\"")
        }
        return emptyList()
    }

    suspend fun fetchContent(queries: List<String>): List<String> {
        val responses = queries.mapNotNull { query ->
            val response = client.post(GRAPHQL_URL) {
                contentType(ContentType.Application.Json)
                setBody(mapOf("query" to query))
            }
            try {
                response.body<FetchContentResponse>()
            } catch (e: Exception) {
                println("Failed to parse response: ${response.bodyAsText()}")
                println("Error: ${e.message}")
                null
            }
        }

        val merged = responses.flatMap { response ->
            response.data.repository.values.map { it.text }
        }

        return merged
    }
}
