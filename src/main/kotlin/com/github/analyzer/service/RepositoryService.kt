package com.github.analyzer.service

import com.github.analyzer.crawlers.JavaRepositoryCrawler
import com.github.analyzer.models.Repository

object RepositoryService {
    suspend fun fetchRepositoriesAndSaveAll(): List<Repository> {
        val repositoryList = JavaRepositoryCrawler.fetchRepositories()
        repositoryList.forEach { repository -> repository.fetchRepoFiles() }
        repositoryList.filter { it.repoFiles.isNotEmpty() }
            .forEach { repository -> repository.fetchClassNames() }

        ElasticsearchService.saveRepositoryBulk(repositoryList)
        return repositoryList
    }

    suspend fun fetchRepositoriesLightAndSaveAll(): List<Repository> {
        // TODO find alternative way to continue from last saved data. Github api doesn't allow to give cursor later.
        // val cursor = ElasticsearchService.getLastCursor() ?: ""
        val repositoryList = JavaRepositoryCrawler.fetchRepositories()
        repositoryList.forEach { repository -> repository.fetchRepoFiles() }
        repositoryList.filter { it.repoFiles.isNotEmpty() }
            .forEach { repository ->
                repository.fetchClassNamesLight()
                repository.saveToElastic()
            }

        return repositoryList
    }
}