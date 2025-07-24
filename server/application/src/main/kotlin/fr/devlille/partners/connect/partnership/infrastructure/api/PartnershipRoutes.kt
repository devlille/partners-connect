package fr.devlille.partners.connect.partnership.infrastructure.api

import fr.devlille.partners.connect.companies.domain.CompanyRepository
import fr.devlille.partners.connect.internal.infrastructure.api.AuthorizedEventPlugin
import fr.devlille.partners.connect.notifications.domain.NotificationRepository
import fr.devlille.partners.connect.notifications.domain.NotificationVariables
import fr.devlille.partners.connect.partnership.domain.PartnershipRepository
import fr.devlille.partners.connect.partnership.domain.PartnershipSuggestionRepository
import fr.devlille.partners.connect.partnership.domain.RegisterPartnership
import fr.devlille.partners.connect.partnership.domain.SuggestPartnership
import fr.devlille.partners.connect.sponsoring.domain.PackRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

@Suppress("ThrowsCount")
fun Route.partnershipRoutes() {
    val packRepository by inject<PackRepository>()
    val companyRepository by inject<CompanyRepository>()
    val partnershipRepository by inject<PartnershipRepository>()
    val notificationRepository by inject<NotificationRepository>()

    route("/events/{eventId}/companies/{companyId}/partnership") {
        post {
            val eventId = call.parameters["eventId"] ?: throw BadRequestException("Missing event id")
            val companyId = call.parameters["companyId"] ?: throw BadRequestException("Missing company id")
            val register = call.receive<RegisterPartnership>()
            val id = partnershipRepository.register(eventId, companyId, register)
            val company = companyRepository.getById(companyId)
            val pack = packRepository.getById(eventId, register.packId, register.language)
            notificationRepository
                .sendMessage(eventId, NotificationVariables.NewPartnership(register.language, pack, company))
            call.respond(HttpStatusCode.Created, mapOf("id" to id))
        }

        route("/{partnershipId}/validate") {
            install(AuthorizedEventPlugin)

            post {
                val eventId = call.parameters["eventId"] ?: throw BadRequestException("Missing event id")
                val companyId = call.parameters["companyId"] ?: throw BadRequestException("Missing company id")
                val partnerId = call.parameters["partnershipId"] ?: throw BadRequestException("Missing partnership id")
                val id = partnershipRepository.validate(eventId, partnerId)
                val company = companyRepository.getById(companyId)
                val partnership = partnershipRepository.getById(eventId, partnerId)
                notificationRepository
                    .sendMessage(eventId, NotificationVariables.PartnershipValidated(partnership.language, company))
                call.respond(HttpStatusCode.OK, mapOf("id" to id))
            }
        }

        route("/{partnershipId}/decline") {
            install(AuthorizedEventPlugin)

            post {
                val eventId = call.parameters["eventId"] ?: throw BadRequestException("Missing event id")
                val companyId = call.parameters["companyId"] ?: throw BadRequestException("Missing company id")
                val partnerId = call.parameters["partnershipId"] ?: throw BadRequestException("Missing partnership id")
                val id = partnershipRepository.decline(eventId, partnerId)
                val company = companyRepository.getById(companyId)
                val partnership = partnershipRepository.getById(eventId, partnerId)
                notificationRepository
                    .sendMessage(eventId, NotificationVariables.PartnershipDeclined(partnership.language, company))
                call.respond(HttpStatusCode.OK, mapOf("id" to id))
            }
        }
    }
}

@Suppress("ThrowsCount")
fun Route.partnershipSuggestionRoutes() {
    val packRepository by inject<PackRepository>()
    val companyRepository by inject<CompanyRepository>()
    val partnershipRepository by inject<PartnershipRepository>()
    val suggestionRepository by inject<PartnershipSuggestionRepository>()
    val notificationRepository by inject<NotificationRepository>()

    route("/events/{eventId}/companies/{companyId}/partnership/{partnershipId}") {
        route("/suggestion") {
            install(AuthorizedEventPlugin)

            post {
                val eventId = call.parameters["eventId"] ?: throw BadRequestException("Missing event id")
                val companyId = call.parameters["companyId"] ?: throw BadRequestException("Missing company id")
                val partnershipId = call.parameters["partnershipId"]
                    ?: throw BadRequestException("Missing partnership id")
                val input = call.receive<SuggestPartnership>()
                val id = suggestionRepository.suggest(eventId, companyId, partnershipId, input)
                val pack = packRepository.getById(eventId, input.packId, input.language)
                val company = companyRepository.getById(companyId)
                notificationRepository
                    .sendMessage(eventId, NotificationVariables.NewSuggestion(input.language, pack, company))
                call.respond(HttpStatusCode.OK, mapOf("id" to id))
            }
        }

        route("/suggestion-approve") {
            post {
                val eventId = call.parameters["eventId"] ?: throw BadRequestException("Missing event id")
                val companyId = call.parameters["companyId"] ?: throw BadRequestException("Missing company id")
                val partnerId = call.parameters["partnershipId"] ?: throw BadRequestException("Missing partnership id")
                val id = suggestionRepository.approve(eventId, companyId, partnerId)
                val company = companyRepository.getById(companyId)
                val partnership = partnershipRepository.getById(eventId, partnerId)
                notificationRepository
                    .sendMessage(eventId, NotificationVariables.SuggestionApproved(partnership.language, company))
                call.respond(HttpStatusCode.OK, mapOf("id" to id))
            }
        }

        route("/suggestion-decline") {
            post {
                val eventId = call.parameters["eventId"] ?: throw BadRequestException("Missing event id")
                val companyId = call.parameters["companyId"] ?: throw BadRequestException("Missing company id")
                val partnerId = call.parameters["partnershipId"] ?: throw BadRequestException("Missing partnership id")
                val id = suggestionRepository.decline(eventId, companyId, partnerId)
                val company = companyRepository.getById(companyId)
                val partnership = partnershipRepository.getById(eventId, partnerId)
                notificationRepository
                    .sendMessage(eventId, NotificationVariables.SuggestionDeclined(partnership.language, company))
                call.respond(HttpStatusCode.OK, mapOf("id" to id))
            }
        }
    }
}
