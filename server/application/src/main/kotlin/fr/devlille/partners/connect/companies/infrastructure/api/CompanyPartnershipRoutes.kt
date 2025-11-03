package fr.devlille.partners.connect.companies.infrastructure.api

import fr.devlille.partners.connect.partnership.domain.PartnershipRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import org.koin.ktor.ext.inject

fun Route.publicCompanyPartnershipRoutes() {
    val partnershipRepository by inject<PartnershipRepository>()

    get("/companies/{companyId}/partnerships") {
        val companyId = call.parameters.companyUUID
        val items = partnershipRepository.listByCompany(companyId)
        call.respond(HttpStatusCode.OK, items)
    }
}
