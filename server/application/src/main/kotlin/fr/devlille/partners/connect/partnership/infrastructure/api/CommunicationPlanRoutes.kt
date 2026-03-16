package fr.devlille.partners.connect.partnership.infrastructure.api

import fr.devlille.partners.connect.events.infrastructure.api.eventSlug
import fr.devlille.partners.connect.internal.infrastructure.ktor.AuthorizedOrganisationPlugin
import fr.devlille.partners.connect.internal.infrastructure.ktor.receive
import fr.devlille.partners.connect.partnership.domain.CommunicationPlanRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject

fun Route.orgsEventCommunicationPlanRoutes() {
    val communicationPlanRepository by inject<CommunicationPlanRepository>()

    route("/orgs/{orgSlug}/events/{eventSlug}/communication-plan") {
        install(AuthorizedOrganisationPlugin)

        post {
            val eventSlug = call.parameters.eventSlug
            val request = call.receive<CommunicationPlanRequest>(schema = "communication_plan_request.schema.json")
            val entry = communicationPlanRepository.create(
                eventSlug = eventSlug,
                title = request.title,
                scheduledDate = request.scheduledDate,
                description = request.description,
                supportUrl = request.supportUrl,
            )
            call.respond(HttpStatusCode.Created, entry)
        }

        route("/{id}") {
            put {
                val eventSlug = call.parameters.eventSlug
                val id = call.parameters.communicationPlanId
                val request = call.receive<CommunicationPlanRequest>(schema = "communication_plan_request.schema.json")
                val entry = communicationPlanRepository.update(
                    eventSlug = eventSlug,
                    id = id,
                    title = request.title,
                    scheduledDate = request.scheduledDate,
                    description = request.description,
                    supportUrl = request.supportUrl,
                )
                call.respond(HttpStatusCode.OK, entry)
            }

            delete {
                val eventSlug = call.parameters.eventSlug
                val id = call.parameters.communicationPlanId
                communicationPlanRepository.delete(eventSlug = eventSlug, id = id)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}

@Serializable
data class CommunicationPlanRequest(
    val title: String,
    @SerialName("scheduled_date")
    val scheduledDate: LocalDateTime? = null,
    val description: String? = null,
    @SerialName("support_url")
    val supportUrl: String? = null,
)
