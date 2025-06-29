package fr.devlille.partners.connect.events.infrastructure.api

import fr.devlille.partners.connect.events.domain.EventRepository
import fr.devlille.partners.connect.events.domain.EventSummaryEntity
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
        call.respond(events.map(EventSummaryEntity::toApi))
    }

    post {
        val request = call.receive<CreateOrUpdateEventRequest>()
        val id = UUID.randomUUID()
        val event = request.toEntity(id)
        repository.createEvent(event)
        call.respond(HttpStatusCode.Created, CreateOrUpdateEventResponse(id.toString()))
    }

    put("/{id}") {
        val id = UUID.fromString(call.parameters["id"]!!)
        val request = call.receive<CreateOrUpdateEventRequest>()
        val event = request.toEntity(id)
        repository.updateEvent(event)
        call.respond(HttpStatusCode.OK, CreateOrUpdateEventResponse(id.toString()))
    }
}
