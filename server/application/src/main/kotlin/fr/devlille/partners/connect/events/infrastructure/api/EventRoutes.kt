package fr.devlille.partners.connect.events.infrastructure.api

import fr.devlille.partners.connect.auth.domain.AuthRepository
import fr.devlille.partners.connect.events.domain.Event
import fr.devlille.partners.connect.events.domain.EventRepository
import fr.devlille.partners.connect.internal.infrastructure.api.UnauthorizedException
import fr.devlille.partners.connect.internal.infrastructure.api.token
import fr.devlille.partners.connect.internal.infrastructure.uuid.toUUID
import fr.devlille.partners.connect.users.domain.UserRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import org.koin.ktor.ext.inject
import kotlin.getValue

fun Route.eventRoutes() {
    val repository by inject<EventRepository>()
    val authRepository by inject<AuthRepository>()
    val userRepository by inject<UserRepository>()

    get {
        call.respond(repository.getAllEvents())
    }

    post {
        val request = call.receive<Event>()
        val token = call.token
        val id = repository.createEvent(request)
        val userInfo = authRepository.getUserInfo(token)
        userRepository.grantUsers(id, listOf(userInfo.email))
        call.respond(
            status = HttpStatusCode.Created,
            message = mapOf("id" to id.toString()),
        )
    }

    put("/{eventId}") {
        val id = call.parameters["eventId"]?.toUUID() ?: throw BadRequestException("Missing id")
        val token = call.token
        val userInfo = authRepository.getUserInfo(token)
        val canEdit = userRepository.hasEditPermissionByEmail(userInfo.email, id)
        if (!canEdit) throw UnauthorizedException("You are not allowed to edit this event")
        val request = call.receive<Event>()
        repository.updateEvent(id, request)
        call.respond(
            status = HttpStatusCode.OK,
            message = mapOf("id" to id.toString()),
        )
    }
}
