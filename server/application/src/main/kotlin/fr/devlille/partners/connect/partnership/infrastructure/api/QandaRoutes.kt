package fr.devlille.partners.connect.partnership.infrastructure.api

import fr.devlille.partners.connect.events.infrastructure.api.eventSlug
import fr.devlille.partners.connect.internal.infrastructure.ktor.WebhookPartnershipPlugin
import fr.devlille.partners.connect.internal.infrastructure.ktor.receive
import fr.devlille.partners.connect.partnership.domain.QandaQuestionRequest
import fr.devlille.partners.connect.partnership.domain.QandaRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

fun Route.qandaRoutes() {
    val repository by inject<QandaRepository>()

    route("/events/{eventSlug}/partnerships/{partnershipId}/qanda/questions") {
        get {
            val partnershipId = call.parameters.partnershipId
            val questions = repository.listByPartnership(partnershipId)
            call.respond(HttpStatusCode.OK, questions)
        }
    }

    route("/events/{eventSlug}/partnerships/{partnershipId}/qanda/questions") {
        install(WebhookPartnershipPlugin)

        post {
            val eventSlug = call.parameters.eventSlug
            val partnershipId = call.parameters.partnershipId
            val request = call.receive<QandaQuestionRequest>(schema = "qanda_question_request.schema.json")
            val question = repository.create(partnershipId, eventSlug, request)
            call.respond(HttpStatusCode.Created, question)
        }
    }

    route("/events/{eventSlug}/partnerships/{partnershipId}/qanda/questions/{questionId}") {
        install(WebhookPartnershipPlugin)

        put {
            val eventSlug = call.parameters.eventSlug
            val partnershipId = call.parameters.partnershipId
            val questionId = call.parameters.questionId
            val request = call.receive<QandaQuestionRequest>(schema = "qanda_question_request.schema.json")
            val question = repository.update(partnershipId, questionId, eventSlug, request)
            call.respond(HttpStatusCode.OK, question)
        }

        delete {
            val partnershipId = call.parameters.partnershipId
            val questionId = call.parameters.questionId
            repository.delete(partnershipId, questionId)
            call.respond(HttpStatusCode.NoContent)
        }
    }

    route("/events/{eventSlug}/qanda/questions") {
        get {
            val eventSlug = call.parameters.eventSlug
            val questions = repository.listByEvent(eventSlug)
            call.respond(HttpStatusCode.OK, questions)
        }
    }
}
