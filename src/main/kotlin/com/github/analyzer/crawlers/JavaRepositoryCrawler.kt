package com.github.analyzer.crawlers


object JavaRepositoryCrawler : RepositoryCrawler {
    override val LANGUAGE: String = "java"
    override val FILE_EXTENSION: String = ".java"
    override val LIMIT: String = "50"
}