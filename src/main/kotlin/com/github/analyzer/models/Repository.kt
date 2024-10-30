package com.github.analyzer.models

import com.github.analyzer.crawlers.JavaRepositoryCrawler
import com.github.analyzer.service.ElasticsearchService
import com.github.analyzer.utils.GraphQL.getFetchFileContentsQuery
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.Serializable

private val logger = KotlinLogging.logger {}

@Serializable
class Repository(
    val name: String,
    val url: String,
    val owner: Owner,
    val primaryLanguage: PrimaryLanguage,
    val branch: Branch
) {
    var repoFiles: List<RepoFile> = emptyList()
    var cursor: String? = null

    suspend fun fetchRepoFiles() {
        if (repoFiles.isEmpty()) {
            repoFiles = JavaRepositoryCrawler.fetchRepositoryFiles(this)
        }
    }

    suspend fun fetchClassNames() {
        val contents: List<String> = JavaRepositoryCrawler.fetchContent(getFetchFileContentsQuery(this))
        val classNames: List<List<String>> = contents.map { getClassNamesFromContent(it) }
        if (classNames.size != repoFiles.size) {
            logger.warn { "Number of fetched class names does not equal the number of repo files." }
        } else {
            classNames.forEachIndexed { index, strings -> repoFiles[index].classNames = strings }
        }
    }

    fun fetchClassNamesLight() {
        val classNames: List<List<String>> = repoFiles.map { getClassNamesFromFilePath(it) }
        if (classNames.size != repoFiles.size) {
            logger.warn { "Number of fetched class names does not equal the number of repo files." }
        } else {
            classNames.forEachIndexed { index, strings -> repoFiles[index].classNames = strings }
        }
    }

    private fun getClassNamesFromContent(content: String): List<String> {
        val regex = Regex("\\b(class|interface)\\s+(\\w+)")

        return regex.findAll(content)
            .map { it.groupValues[1] }
            .toList()
    }

    private fun getClassNamesFromFilePath(repoFile: RepoFile): List<String> {
        if (!repoFile.path.contains(".java")) {
            return emptyList()
        }
        return listOf(repoFile.path.substringAfterLast('/').substringBefore(".java"))
    }

    fun saveToElastic() {
        ElasticsearchService.saveRepository(this)
    }

    @Serializable
    data class PrimaryLanguage(val name: String)

    @Serializable
    data class Owner(val username: String)

    @Serializable
    data class Branch(val name: String)
}