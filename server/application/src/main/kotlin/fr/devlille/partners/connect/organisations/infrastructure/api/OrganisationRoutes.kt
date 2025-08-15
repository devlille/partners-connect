package fr.devlille.partners.connect.organisations.infrastructure.api

import fr.devlille.partners.connect.organisations.domain.Organisation
import fr.devlille.partners.connect.organisations.domain.OrganisationRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

fun Route.organisationRoutes() {
    val repository by inject<OrganisationRepository>()

    route("/orgs") {
        post {
            val input = call.receive<Organisation>()
            val slug = repository.create(input)
            call.respond(HttpStatusCode.Created, mapOf("slug" to slug))
        }

        get("/{slug}") {
            val slug = call.parameters["slug"] ?: throw BadRequestException("Missing slug")
            call.respond(HttpStatusCode.OK, repository.getById(slug))
        }
    }
}
