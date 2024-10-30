package com.github.analyzer.plugins

import com.github.analyzer.service.RepositoryService
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*


fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Welcome to the GitHub analyzer.")
        }

        get("/save-repositories") {
            val repositories = RepositoryService.fetchRepositoriesAndSaveAll()
            call.respond(repositories)
        }

        get("/save-repositories-light") {
            val repositories = RepositoryService.fetchRepositoriesLightAndSaveAll()
            call.respond(repositories)
        }
    }
}
