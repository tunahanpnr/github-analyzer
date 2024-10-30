package com.github.analyzer.plugins

import com.github.analyzer.service.ElasticsearchService
import com.github.analyzer.service.RepositoryService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*


fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Welcome to the GitHub analyzer.")
        }

        get("/repositories") {
            val repositories = RepositoryService.fetchClassNamesAndSaveAll()
            call.respond(repositories)
        }

        get("/repositories/light") {
            val repositories = RepositoryService.fetchClassNamesLightAndSaveAll()
            call.respond(repositories)
        }

        get("/search") {
            // Get the search term from the query parameters
            val keyword = call.request.queryParameters["keyword"]

            if (keyword == null || keyword.isBlank()) {
                call.respond(HttpStatusCode.BadRequest, "Query parameter 'keyword' is required")
                return@get
            }

            // Call the search function
            val searchResults = ElasticsearchService.searchRepositories(keyword)

            // Return the results as JSON
            call.respond(searchResults)
        }
    }
}
