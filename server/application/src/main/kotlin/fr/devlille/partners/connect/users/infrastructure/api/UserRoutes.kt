package fr.devlille.partners.connect.users.infrastructure.api

import fr.devlille.partners.connect.auth.domain.AuthRepository
import fr.devlille.partners.connect.events.domain.EventRepository
import fr.devlille.partners.connect.internal.infrastructure.api.UnauthorizedException
import fr.devlille.partners.connect.internal.infrastructure.api.token
import fr.devlille.partners.connect.internal.infrastructure.ktor.receive
import fr.devlille.partners.connect.organisations.domain.OrganisationRepository
import fr.devlille.partners.connect.organisations.infrastructure.api.orgSlug
import fr.devlille.partners.connect.users.domain.UserRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

fun Route.userRoutes() {
    val authRepository by inject<AuthRepository>()
    val userRepository by inject<UserRepository>()
    val eventRepository by inject<EventRepository>()
    val organisationRepository by inject<OrganisationRepository>()

    route("/users") {
        get("/me/events") {
            val token = call.token
            val userInfo = authRepository.getUserInfo(token)
            val events = eventRepository.findByUserEmail(userInfo.email)
            call.respond(HttpStatusCode.OK, events)
        }

        get("/me/orgs") {
            val token = call.token
            val userInfo = authRepository.getUserInfo(token)
            val organisations = organisationRepository.findOrganisationListByUserEmail(userInfo.email)
            call.respond(HttpStatusCode.OK, organisations)
        }
    }

    route("/orgs/{orgSlug}/users") {
        get {
            val orgSlug = call.parameters.orgSlug
            call.respond(HttpStatusCode.OK, userRepository.findUsersByOrgSlug(orgSlug))
        }
        post("/grant") {
            val token = call.token
            val orgSlug = call.parameters.orgSlug
            val request = call.receive<GrantPermissionRequest>(schema = "grant_permission_request.schema.json")
            val userInfo = authRepository.getUserInfo(token)
            val hasPerm = userRepository.hasEditPermissionByEmail(userInfo.email, orgSlug)
            if (!hasPerm) {
                throw UnauthorizedException("You do not have permission to grant users for this event")
            }
            userRepository.grantUsers(orgSlug, request.userEmails)
            call.respond(HttpStatusCode.OK, "Permissions granted")
        }
    }
}
