package com.github.analyzer.models

import com.github.analyzer.crawlers.JavaRepositoryCrawler
import kotlinx.serialization.Serializable


@Serializable
class Repository(
    val name: String,
    val url: String,
    val owner: Owner,
    val primaryLanguage: PrimaryLanguage,
    val branch: Branch
) {
    var repoFiles: List<RepoFile> = emptyList()

    suspend fun setRepoFiles() {
        if (repoFiles.isEmpty()) {
            repoFiles = JavaRepositoryCrawler.fetchRepositoryFiles(this)
        }
    }

    suspend fun setClassNameOfTheRepoFiles() {
        val contents: List<String> = JavaRepositoryCrawler.fetchContent(getFetchFileContentsQuery())
        val classNames: List<List<String>> = contents.map { getClassNamesFromJavaFile(it) }
        if (classNames.size != repoFiles.size) {
            print("Number of fetched class names does not equal the number of repo files.")
        } else {
            classNames.forEachIndexed { index, strings -> repoFiles[index].classNames = strings }
        }
    }

    private fun getFetchFileContentsQuery(): List<String> {
        val chunks = repoFiles.chunked(250)
        return chunks.map { getFetchFileContentsQuery(it) }
    }

    private fun getFetchFileContentsQuery(chunk: List<RepoFile>): String {
        return """
            query RepoFiles {
              repository(owner: "${owner.username}", name: "$name") {
                ${chunk.mapIndexed { index, repoFile -> repoFile.getFetchFileQuery(index.toString()) }.joinToString("\n")}
              }
            }
        """.trimIndent()
    }

    private fun getClassNamesFromJavaFile(content: String): List<String> {
        val classRegex = Regex("\\bclass\\s+(\\w+)")

        return classRegex.findAll(content)
            .map { it.groupValues[1] }
            .toList()
    }

    @Serializable
    data class PrimaryLanguage(val name: String)

    @Serializable
    data class Owner(val username: String)

    @Serializable
    data class Branch(val name: String)
}