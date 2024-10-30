package com.github.analyzer.utils

import com.github.analyzer.models.RepoFile
import com.github.analyzer.models.Repository

object GraphQL {
    // max allowed limit from GitHub is 100, otherwise query will throw error
    private const val CHUNK_SIZE = 100

    fun getGraphQLBaseURL(): String {
        return "https://api.github.com/graphql"
    }

    fun getRepositoryQuery(language: String, after: String?): String {
        return """
                   query {
                       search(query: "language:$language", type: REPOSITORY, first: $CHUNK_SIZE, after: "$after") {
                           edges {
                                   node {
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
                                   cursor
                           }
                           pageInfo {
                                endCursor
                                hasNextPage
                           }
                       }
                   }
               """.trimIndent()
    }

    fun getFetchFileContentsQuery(repository: Repository): List<String> {
        val chunks = repository.repoFiles.chunked(CHUNK_SIZE)
        return chunks.map { getFetchFileContentsQuery(it, repository.owner.username, repository.name) }
    }

    fun getFetchFileContentsQuery(repoFiles: List<RepoFile>, username: String, name: String): String {
        return """
            query RepoFiles {
              repository(owner: "${username}", name: "$name") {
                ${
            repoFiles.mapIndexed { index, repoFile -> repoFile.getFetchFileQuery(index.toString()) }.joinToString("\n")
        }
              }
            }
        """.trimIndent()
    }
}