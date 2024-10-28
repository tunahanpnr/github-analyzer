package com.github.analyzer.plugins

import com.github.analyzer.service.RepositoryService
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*


fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Hello World!")
        }

        get("/repositories") {
            val repositories = RepositoryService.fetchAndSaveAll()
            call.respond(repositories)
        }
    }
}
