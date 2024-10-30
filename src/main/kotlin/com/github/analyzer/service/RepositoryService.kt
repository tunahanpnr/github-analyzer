package com.github.analyzer.service

import com.github.analyzer.crawlers.JavaRepositoryCrawler
import com.github.analyzer.models.Repository

object RepositoryService {
    suspend fun fetchClassNamesAndSaveAll(): List<Repository> {
        val repositoryList = JavaRepositoryCrawler.fetchRepositories()
        repositoryList.forEach { repository -> repository.fetchRepoFiles() }
        repositoryList.filter { it.repoFiles.isNotEmpty() }
            .forEach { repository -> repository.fetchClassNames() }

        ElasticsearchService.saveRepositoryBulk(repositoryList)
        return repositoryList
    }

    suspend fun fetchClassNamesLightAndSaveAll(): List<Repository> {
        val repositoryList = JavaRepositoryCrawler.fetchRepositories()
        repositoryList.forEach { repository -> repository.fetchRepoFiles() }
        repositoryList.filter { it.repoFiles.isNotEmpty() }
            .forEach { repository -> repository.fetchClassNamesLight() }

        ElasticsearchService.saveRepositoryBulk(repositoryList)

        return repositoryList
    }
}