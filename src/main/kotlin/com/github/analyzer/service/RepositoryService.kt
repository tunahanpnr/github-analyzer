package com.github.analyzer.service

import com.github.analyzer.crawlers.JavaRepositoryCrawler
import com.github.analyzer.models.Repository

object RepositoryService {
    suspend fun fetchAndSaveAll(): List<Repository> {
        val repositoryList = JavaRepositoryCrawler.fetchRepositories()
        repositoryList.forEach { repository -> repository.setRepoFiles() }
        repositoryList.filter { it.repoFiles.isNotEmpty() }
            .forEach { repository -> repository.setClassNameOfTheRepoFiles() }

        return repositoryList
    }
}