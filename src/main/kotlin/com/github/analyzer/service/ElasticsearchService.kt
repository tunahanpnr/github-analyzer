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

private val logger = KotlinLogging.logger {}

object ElasticsearchService {

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
                    logger.error{ err.reason() }
                }
            }
        }
    }
}
