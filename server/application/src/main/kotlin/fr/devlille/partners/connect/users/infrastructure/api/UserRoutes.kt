package fr.devlille.partners.connect.users.infrastructure.api

import fr.devlille.partners.connect.auth.domain.AuthRepository
import fr.devlille.partners.connect.internal.infrastructure.api.UnauthorizedException
import fr.devlille.partners.connect.internal.infrastructure.api.token
import fr.devlille.partners.connect.internal.infrastructure.system.SystemVarEnv
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

fun Route.userRoutes() {
    val authRepository by inject<AuthRepository>()
    val userRepository by inject<UserRepository>()

    route("/events/{eventId}/users") {
        get {
            val eventId = call.parameters["eventId"] ?: throw BadRequestException("event id is required")
            call.respond(
                HttpStatusCode.OK,
                userRepository.findUsersByEventId(eventId)
            )
        }
        post("/grant") {
            val eventId = call.parameters["eventId"] ?: throw BadRequestException("event id is required")
            val request = call.receive<GrantPermissionRequest>()
            val token = call.token
            val userInfo = authRepository.getUserInfo(token)
            val hasPerm = userRepository.hasEditPermissionByEmail(userInfo.email, eventId)
            if (!hasPerm || SystemVarEnv.owner == userInfo.email) {
                throw UnauthorizedException("You do not have permission to grant users for this event")
            }
            userRepository.grantUsers(eventId, request.userIds)
            call.respond(HttpStatusCode.OK, "Permissions granted")
        }
    }
}
