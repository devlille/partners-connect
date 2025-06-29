package fr.devlille.partners.connect.events.infrastructure.api

import fr.devlille.partners.connect.events.domain.EventRepository
import fr.devlille.partners.connect.events.domain.EventSummary
import fr.devlille.partners.connect.events.infrastructure.api.mappers.toResponse
import fr.devlille.partners.connect.events.infrastructure.api.mappers.toDomain
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import org.koin.ktor.ext.inject
import java.util.UUID

fun Route.eventRoutes() {
    val repository by inject<EventRepository>()

    get {
        val events = repository.getAllEvents()
        call.respond(events.map(EventSummary::toResponse))
    }

    post {
        val request = call.receive<CreateOrUpdateEventRequest>()
        call.respond(
            status = HttpStatusCode.Created,
            message = CreateOrUpdateEventResponse(repository.createEvent(request.toDomain()).toString())
        )
    }

    put("/{id}") {
        val id = UUID.fromString(call.parameters["id"]!!)
        val request = call.receive<CreateOrUpdateEventRequest>()
        call.respond(
            status = HttpStatusCode.OK,
            message = CreateOrUpdateEventResponse(repository.updateEvent(request.toDomain(id)).toString())
        )
    }
}
