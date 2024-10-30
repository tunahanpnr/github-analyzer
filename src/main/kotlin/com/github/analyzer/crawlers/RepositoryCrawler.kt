package com.github.analyzer.crawlers

import com.github.analyzer.models.RepoFile
import com.github.analyzer.models.Repository
import com.github.analyzer.utils.GraphQL.getGraphQLBaseURL
import com.github.analyzer.utils.GraphQL.getRepositoryQuery
import io.github.oshai.kotlinlogging.KotlinLogging
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

private val logger = KotlinLogging.logger {}

interface RepositoryCrawler {
    @Serializable
    data class SearchResponse(
        val data: Data
    )

    @Serializable
    data class Data(
        val search: SearchData
    )

    @Serializable
    data class SearchData(
        val edges: List<Edge>,
        val pageInfo: PageInfo
    )

    @Serializable
    data class Edge(
        val node: Repository,
        val cursor: String
    )

    @Serializable
    data class PageInfo(
        val endCursor: String,
        val hasNextPage: Boolean,
    )

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
    val LIMIT: Int

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
        var lastCursor = ""
        val repositories = mutableListOf<Repository>()

        while (repositories.size < LIMIT) {
            val query = getRepositoryQuery(LANGUAGE, lastCursor)

            val response = client.post(getGraphQLBaseURL()) {
                contentType(ContentType.Application.Json)
                setBody(mapOf("query" to query))
            }

            val nodes: List<Repository> = response.body<SearchResponse>().data.search.edges.map {
                it.node.cursor = it.cursor
                it.node
            }
            repositories.addAll(nodes)

            if (!response.body<SearchResponse>().data.search.pageInfo.hasNextPage || repositories.size >= LIMIT) {
                break
            }
            lastCursor = response.body<SearchResponse>().data.search.pageInfo.endCursor
        }

        return repositories.take(LIMIT)
    }

    suspend fun fetchRepositoryFiles(repository: Repository): List<RepoFile> {
        try {
            val response =
                client.get("https://api.github.com/repos/${repository.owner.username}/${repository.name}/git/trees/${repository.branch.name}?recursive=1") {
                    contentType(ContentType.Application.Json)
                }.body<FetchRepoFilesResponse>()
            return response.tree.filter { it.path.removeSurrounding("\"").endsWith(FILE_EXTENSION) }
        } catch (e: HttpRequestTimeoutException) {
            logger.error { "Request timeout: \"https://api.github.com/repos/${repository.owner.username}/${repository.name}/git/trees/${repository.branch.name}?recursive=1\"" }
        }
        return emptyList()
    }

    suspend fun fetchContent(queries: List<String>): List<String> {
        val responses = queries.mapNotNull { query ->
            val response = client.post(getGraphQLBaseURL()) {
                contentType(ContentType.Application.Json)
                setBody(mapOf("query" to query))
            }
            try {
                response.body<FetchContentResponse>()
            } catch (e: Exception) {
                val text = response.bodyAsText()
                logger.error { "Failed to parse response: $text, error: ${e.message}" }
                null
            }
        }

        val merged = responses.flatMap { response ->
            response.data.repository.values.map { it.text }
        }

        return merged
    }
}
