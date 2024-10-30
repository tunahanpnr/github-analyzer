package com.github.analyzer.service

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.elasticsearch.core.BulkRequest.Builder
import co.elastic.clients.elasticsearch.core.BulkResponse
import co.elastic.clients.elasticsearch.core.IndexRequest
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation
import co.elastic.clients.elasticsearch.core.bulk.IndexOperation
import co.elastic.clients.transport.rest_client.RestClientTransport
import com.github.analyzer.models.Repository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.http.HttpHost
import org.elasticsearch.client.RestClient
import co.elastic.clients.elasticsearch._types.query_dsl.Query
import co.elastic.clients.elasticsearch.core.SearchRequest
import co.elastic.clients.elasticsearch.core.SearchResponse

private val logger = KotlinLogging.logger {}

object ElasticsearchService {

    private const val BATCH_SIZE: Int = 10
    private val esClient: ElasticsearchClient

    init {
        val restClient: RestClient = RestClient.builder(HttpHost("localhost", 9200, "http")).build()
        val transport = RestClientTransport(restClient, co.elastic.clients.json.jackson.JacksonJsonpMapper())
        esClient = ElasticsearchClient(transport)
    }

    fun saveRepository(repository: Repository) {
        val indexRequest = IndexRequest.of { i ->
            i
                .index("repositories")
                .id(repository.url)
                .document(repository)
        }

        esClient.index(indexRequest)
    }

    fun saveRepositoryBulk(repositories: List<Repository>) {
        repositories.chunked(BATCH_SIZE).forEach { _ ->
            val br = Builder()
            for (repository in repositories) {
                br.operations { op: BulkOperation.Builder ->
                    op
                        .index { idx: IndexOperation.Builder<Any?> ->
                            idx.index("repositories")
                                .id(repository.name)
                                .document(repository)
                        }
                }
            }

            val result: BulkResponse = esClient.bulk(br.build())

            if (result.errors()) {
                for (item in result.items()) {
                    val err = item.error()
                    if (err != null) {
                        logger.error { err.reason() }
                    }
                }
            }
        }
    }

    fun searchRepositories(keyword: String, field: String = "name"): List<Repository> {
        // Define the search query
        val searchQuery = Query.of { q ->
            q.match { m ->
                m.field(field)
                    .query(keyword)
            }
        }

        // Build the search request
        val searchRequest = SearchRequest.of { sr ->
            sr.index("repositories")
                .query(searchQuery)
                .size(100) // Set a limit on the number of results
        }

        // Execute the search request
        val response: SearchResponse<Repository> = esClient.search(searchRequest, Repository::class.java)

        // Process the response and return the list of repositories
        return response.hits().hits().map { it.source()!! } // Map hits to Repository objects
    }
}
