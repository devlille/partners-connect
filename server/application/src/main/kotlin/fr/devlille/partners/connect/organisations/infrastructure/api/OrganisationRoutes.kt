package fr.devlille.partners.connect.organisations.infrastructure.api

import fr.devlille.partners.connect.auth.domain.AuthRepository
import fr.devlille.partners.connect.internal.infrastructure.api.token
import fr.devlille.partners.connect.organisations.domain.Organisation
import fr.devlille.partners.connect.organisations.domain.OrganisationRepository
import fr.devlille.partners.connect.users.domain.UserRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject
import kotlin.getValue

fun Route.organisationRoutes() {
    val repository by inject<OrganisationRepository>()
    val authRepository by inject<AuthRepository>()
    val userRepository by inject<UserRepository>()

    route("/orgs") {
        post {
            val input = call.receive<Organisation>()
            val token = call.token
            val slug = repository.create(input)
            val userInfo = authRepository.getUserInfo(token)
            userRepository.grantUsers(slug, listOf(userInfo.email))
            call.respond(HttpStatusCode.Created, mapOf("slug" to slug))
        }

        get("/{slug}") {
            val slug = call.parameters["slug"] ?: throw BadRequestException("Missing slug")
            call.respond(HttpStatusCode.OK, repository.getById(slug))
        }
    }
}
