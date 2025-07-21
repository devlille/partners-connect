package fr.devlille.partners.connect.partnership.infrastructure.api

import fr.devlille.partners.connect.companies.domain.CompanyRepository
import fr.devlille.partners.connect.integrations.domain.NotificationRepository
import fr.devlille.partners.connect.partnership.domain.PartnershipRepository
import fr.devlille.partners.connect.partnership.domain.RegisterPartnership
import fr.devlille.partners.connect.sponsoring.domain.PackRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import org.koin.ktor.ext.inject

fun Route.partnershipRoutes() {
    val packRepository by inject<PackRepository>()
    val companyRepository by inject<CompanyRepository>()
    val partnershipRepository by inject<PartnershipRepository>()
    val notificationRepository by inject<NotificationRepository>()

    post("/events/{eventId}/companies/{companyId}/partnership") {
        val eventId = call.parameters["eventId"] ?: throw BadRequestException("Missing event id")
        val companyId = call.parameters["companyId"] ?: throw BadRequestException("Missing company id")
        val register = call.receive<RegisterPartnership>()
        val id = partnershipRepository.register(eventId, companyId, register)
        val company = companyRepository.getById(companyId)
        val pack = packRepository.getById(eventId, register.packId, register.language)
        notificationRepository
            .sendMessage(eventId, "New partnership for pack ${pack.name} registered for company ${company.name}")
        call.respond(HttpStatusCode.Created, mapOf("id" to id))
    }
}
