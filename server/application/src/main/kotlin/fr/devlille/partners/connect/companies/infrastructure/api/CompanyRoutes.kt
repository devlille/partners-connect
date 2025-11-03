package fr.devlille.partners.connect.companies.infrastructure.api

import fr.devlille.partners.connect.companies.domain.Company
import fr.devlille.partners.connect.companies.domain.CompanyRepository
import fr.devlille.partners.connect.companies.domain.CompanyStatus
import fr.devlille.partners.connect.companies.domain.CreateCompany
import fr.devlille.partners.connect.companies.domain.UpdateCompany
import fr.devlille.partners.connect.internal.infrastructure.api.DEFAULT_PAGE_SIZE
import fr.devlille.partners.connect.internal.infrastructure.ktor.receive
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

fun Route.companyRoutes() {
    publicCompanyRoutes()
    publicCompanyMediaRoutes()
    publicCompanyPartnershipRoutes()
    publicCompanyJobOfferRoutes()
    publicCompanyJobOfferPromotionsRoutes()
}

private fun Route.publicCompanyRoutes() {
    val companyRepository by inject<CompanyRepository>()

    route("/companies") {
        get {
            val query = call.request.queryParameters["query"]?.trim()
            val status = call.request.queryParameters["filter[status]"]
                ?.let {
                    val status = runCatching { CompanyStatus.valueOf(it.uppercase()) }
                    if (status.isFailure) {
                        throw BadRequestException("Company status '$it' is invalid")
                    }
                    status.getOrNull()
                }
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val pageSize = call.request.queryParameters["page_size"]?.toIntOrNull() ?: DEFAULT_PAGE_SIZE
            val companies = companyRepository.listPaginated(query, status, page, pageSize)
            call.respond(companies)
        }

        post {
            val input = call.receive<CreateCompany>(schema = "create_company.schema.json")
            val id = companyRepository.createOrUpdate(input)
            call.respond(HttpStatusCode.Created, mapOf("id" to id.toString()))
        }

        put("/{companyId}") {
            val companyId = call.parameters.companyUUID
            val input = call.receive<UpdateCompany>(schema = "update_company.schema.json")
            val updatedCompany = companyRepository.update(companyId, input)
            call.respond<Company>(HttpStatusCode.OK, updatedCompany)
        }

        delete("/{companyId}") {
            val companyId = call.parameters.companyUUID
            companyRepository.softDelete(companyId)
            call.respond(HttpStatusCode.NoContent)
        }

        get("/{companyId}") {
            val companyId = call.parameters.companyUUID
            val company = companyRepository.getById(companyId)
            call.respond(HttpStatusCode.OK, company)
        }
    }
}
