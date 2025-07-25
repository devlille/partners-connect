package fr.devlille.partners.connect.events.infrastructure.api

import fr.devlille.partners.connect.events.domain.Event
import fr.devlille.partners.connect.events.domain.EventRepository
import fr.devlille.partners.connect.internal.infrastructure.uuid.toUUID
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import org.koin.ktor.ext.inject

fun Route.eventRoutes() {
    val repository by inject<EventRepository>()

    get {
        call.respond(repository.getAllEvents())
    }

    post {
        val request = call.receive<Event>()
        val id = repository.createEvent(request)
        call.respond(
            status = HttpStatusCode.Created,
            message = mapOf("id" to id.toString()),
        )
    }

    put("/{id}") {
        val id = call.parameters["id"]?.toUUID() ?: throw BadRequestException("Missing id")
        val request = call.receive<Event>()
        repository.updateEvent(id, request)
        call.respond(
            status = HttpStatusCode.OK,
            message = mapOf("id" to id.toString()),
        )
    }
}
