package com.github.analyzer.plugins

import com.github.analyzer.crawlers.JavaRepositoryCrawler
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*


fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Hello World!")
        }

        get("/repositories") {
            val javaRepositoryCrawler = JavaRepositoryCrawler()
            val repositories = javaRepositoryCrawler.searchRepositories()
            call.respond(repositories)
        }
    }
}
