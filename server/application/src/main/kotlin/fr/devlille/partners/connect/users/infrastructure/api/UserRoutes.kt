package fr.devlille.partners.connect.users.infrastructure.api

import fr.devlille.partners.connect.auth.domain.AuthRepository
import fr.devlille.partners.connect.events.domain.EventRepository
import fr.devlille.partners.connect.internal.infrastructure.api.UnauthorizedException
import fr.devlille.partners.connect.internal.infrastructure.api.token
import fr.devlille.partners.connect.internal.infrastructure.system.SystemVarEnv
import fr.devlille.partners.connect.users.domain.UserRepository
import fr.devlille.partners.connect.users.infrastructure.db.UserEntity
import fr.devlille.partners.connect.users.infrastructure.db.singleUserByEmail
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.koin.ktor.ext.inject

@Suppress("ThrowsCount")
fun Route.userRoutes() {
    val authRepository by inject<AuthRepository>()
    val userRepository by inject<UserRepository>()
    val eventRepository by inject<EventRepository>()

    route("/users") {
        get("/me/events") {
            val token = call.token
            val userInfo = authRepository.getUserInfo(token)

            // Find the user in the database
            val user = transaction {
                UserEntity.singleUserByEmail(userInfo.email)
                    ?: throw NotFoundException("User not found")
            }

            // Check if user has any organizer permissions
            val hasOrganizerRole = userRepository.hasAnyOrganizerPermission(userInfo.email)

            if (!hasOrganizerRole) {
                throw UnauthorizedException("You do not have organizer permissions")
            }

            val events = eventRepository.findByOrganizerId(user.id.value)
            call.respond(HttpStatusCode.OK, events)
        }
    }

    route("/orgs/{orgSlug}/users") {
        get {
            val orgSlug = call.parameters["orgSlug"] ?: throw BadRequestException("Orga slug is required")
            call.respond(HttpStatusCode.OK, userRepository.findUsersByOrgSlug(orgSlug))
        }
        post("/grant") {
            val orgSlug = call.parameters["orgSlug"] ?: throw BadRequestException("Orga slug is required")
            val request = call.receive<GrantPermissionRequest>()
            val token = call.token
            val userInfo = authRepository.getUserInfo(token)
            val hasPerm = userRepository.hasEditPermissionByEmail(userInfo.email, orgSlug)
            if (!hasPerm && SystemVarEnv.owner != userInfo.email) {
                throw UnauthorizedException("You do not have permission to grant users for this event")
            }
            userRepository.grantUsers(orgSlug, request.userEmails)
            call.respond(HttpStatusCode.OK, "Permissions granted")
        }
    }
}
