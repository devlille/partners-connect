package fr.devlille.partners.connect.partnership.infrastructure.api

import fr.devlille.partners.connect.internal.infrastructure.ktor.WebhookPartnershipPlugin
import fr.devlille.partners.connect.internal.infrastructure.ktor.receive
import fr.devlille.partners.connect.partnership.domain.BoothActivityRepository
import fr.devlille.partners.connect.partnership.domain.BoothActivityRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

fun Route.boothActivityRoutes() {
    val repository by inject<BoothActivityRepository>()

    route("/events/{eventSlug}/partnerships/{partnershipId}/activities") {
        get {
            val partnershipId = call.parameters.partnershipId
            val activities = repository.list(partnershipId)
            call.respond(HttpStatusCode.OK, activities)
        }
    }

    route("/events/{eventSlug}/partnerships/{partnershipId}/activities") {
        install(WebhookPartnershipPlugin)

        post {
            val partnershipId = call.parameters.partnershipId
            val request = call.receive<BoothActivityRequest>(schema = "booth_activity_request.schema.json")
            val startTime = request.startTime
            val endTime = request.endTime
            if (startTime != null && endTime != null && startTime >= endTime) {
                throw BadRequestException("startTime must be before endTime")
            }
            val activity = repository.create(partnershipId, request)
            call.respond(HttpStatusCode.Created, activity)
        }
    }

    route("/events/{eventSlug}/partnerships/{partnershipId}/activities/{activityId}") {
        install(WebhookPartnershipPlugin)

        put {
            val partnershipId = call.parameters.partnershipId
            val activityId = call.parameters.activityId
            val request = call.receive<BoothActivityRequest>(schema = "booth_activity_request.schema.json")
            val startTime = request.startTime
            val endTime = request.endTime
            if (startTime != null && endTime != null && startTime >= endTime) {
                throw BadRequestException("startTime must be before endTime")
            }
            val activity = repository.update(partnershipId, activityId, request)
            call.respond(HttpStatusCode.OK, activity)
        }

        delete {
            val partnershipId = call.parameters.partnershipId
            val activityId = call.parameters.activityId
            repository.delete(partnershipId, activityId)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
