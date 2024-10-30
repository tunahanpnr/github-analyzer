package com.github.analyzer.service

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.elasticsearch._types.SortOrder
import co.elastic.clients.elasticsearch.core.BulkRequest.Builder
import co.elastic.clients.elasticsearch.core.BulkResponse
import co.elastic.clients.elasticsearch.core.IndexRequest
import co.elastic.clients.elasticsearch.core.SearchRequest
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation
import co.elastic.clients.elasticsearch.core.bulk.IndexOperation
import co.elastic.clients.elasticsearch.indices.ExistsRequest
import co.elastic.clients.elasticsearch.ingest.Processor
import co.elastic.clients.elasticsearch.ingest.PutPipelineRequest
import co.elastic.clients.json.JsonData
import co.elastic.clients.transport.rest_client.RestClientTransport
import com.github.analyzer.models.Repository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.http.HttpHost
import org.elasticsearch.client.RestClient


private val logger = KotlinLogging.logger {}

object ElasticsearchService {

    private const val BATCH_SIZE: Int = 10
    private const val TIMESTAMP_PIPELINE_NAME: String = "timestamp"
    private const val REPOSITORY_INDEX: String = "repository"
    private val esClient: ElasticsearchClient

    init {
        val restClient: RestClient = RestClient.builder(
            HttpHost(
                System.getenv("ELASTICSEARCH_URL"),
                System.getenv("ELASTICSEARCH_PORT").toInt(),
                "http"
            )
        ).build()
        val transport = RestClientTransport(restClient, co.elastic.clients.json.jackson.JacksonJsonpMapper())
        esClient = ElasticsearchClient(transport)

        createTimestampPipeline()
    }

    private fun createTimestampPipeline() {
        val timestampProcessor: Processor = Processor.of { b0 ->
            b0.set { b1 ->
                b1.field(TIMESTAMP_PIPELINE_NAME).value(JsonData.of<String>("{{{_ingest.timestamp}}}"))
            }
        }
        val pipeline = PutPipelineRequest.Builder()
            .id(TIMESTAMP_PIPELINE_NAME)
            .description("Add $TIMESTAMP_PIPELINE_NAME to each document")
            .processors(timestampProcessor)
            .build()

        esClient.ingest().putPipeline(pipeline)
        logger.info { "Ingest pipeline '$TIMESTAMP_PIPELINE_NAME' created." }
        println("Ingest pipeline '$TIMESTAMP_PIPELINE_NAME' created.")
    }

    fun saveRepository(repository: Repository) {
        val indexRequest = IndexRequest.of { i ->
            i
                .index(REPOSITORY_INDEX)
                .id(repository.url)
                .document(repository)
                .pipeline(TIMESTAMP_PIPELINE_NAME)
        }

        esClient.index(indexRequest)
    }

    fun saveRepositoryBulk(repositories: List<Repository>) {
        repositories.chunked(BATCH_SIZE).forEach { chunk ->
            val br = Builder()
            for (repository in chunk) {
                br.operations { op: BulkOperation.Builder ->
                    op
                        .index { idx: IndexOperation.Builder<Any?> ->
                            idx.index(REPOSITORY_INDEX)
                                .id(repository.url)
                                .document(repository)
                                .pipeline(TIMESTAMP_PIPELINE_NAME)
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

    private fun getLastWrittenRepository(): Repository? {
        val indexExistsResponse = esClient.indices().exists(ExistsRequest.Builder().index(REPOSITORY_INDEX).build())
        if (!indexExistsResponse.value()) {
            return null
        }

        val searchRequest = SearchRequest.of { s ->
            s.index(REPOSITORY_INDEX) // Specify the index
            s.size(1)
            s.sort {
                it.field { f ->
                    f.field(TIMESTAMP_PIPELINE_NAME)
                    f.order(SortOrder.Desc)
                }
            }
        }

        val searchResponse = esClient.search(searchRequest, Repository::class.java)
        if (searchResponse.hits().hits().isEmpty()) {
            return null
        }
        return searchResponse.hits().hits().first().source()
    }

    fun getLastCursor(): String? {
        val repo = getLastWrittenRepository() ?: return null
        return repo.cursor
    }
}
