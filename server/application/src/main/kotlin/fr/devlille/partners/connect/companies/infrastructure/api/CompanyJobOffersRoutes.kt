package fr.devlille.partners.connect.companies.infrastructure.api

import fr.devlille.partners.connect.companies.domain.CompanyJobOfferPromotionRepository
import fr.devlille.partners.connect.companies.domain.CompanyJobOfferRepository
import fr.devlille.partners.connect.companies.domain.CreateJobOffer
import fr.devlille.partners.connect.companies.domain.PromoteJobOfferRequest
import fr.devlille.partners.connect.companies.domain.UpdateJobOffer
import fr.devlille.partners.connect.internal.infrastructure.api.DEFAULT_PAGE_SIZE
import fr.devlille.partners.connect.internal.infrastructure.ktor.receive
import fr.devlille.partners.connect.internal.infrastructure.uuid.toUUID
import fr.devlille.partners.connect.partnership.infrastructure.api.partnershipId
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

fun Route.publicCompanyJobOfferRoutes() {
    val jobOfferRepository by inject<CompanyJobOfferRepository>()

    route("/companies/{companyId}/job-offers") {
        post {
            val companyId = call.parameters.companyUUID
            val createJobOffer = call.receive<CreateJobOffer>(schema = "create_job_offer.schema.json")
            val jobOfferId = jobOfferRepository.create(companyId, createJobOffer)
            call.respond(HttpStatusCode.Created, mapOf("id" to jobOfferId.toString()))
        }

        get {
            val companyId = call.parameters.companyUUID
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val pageSize = call.request.queryParameters["page_size"]?.toIntOrNull() ?: DEFAULT_PAGE_SIZE
            val jobOffers = jobOfferRepository.findByCompany(companyId, page, pageSize)
            call.respond(HttpStatusCode.OK, jobOffers)
        }

        get("/{jobOfferId}") {
            val companyId = call.parameters.companyUUID
            val jobOfferId = call.parameters.jobOfferUUID
            val jobOffer = jobOfferRepository.findById(jobOfferId)
            if (jobOffer.companyId != companyId.toString()) {
                throw NotFoundException("Job offer not found or not owned by company")
            }
            call.respond(HttpStatusCode.OK, jobOffer)
        }

        put("/{jobOfferId}") {
            val companyId = call.parameters.companyUUID
            val jobOfferId = call.parameters.jobOfferUUID
            val updateJobOffer = call.receive<UpdateJobOffer>(schema = "update_job_offer.schema.json")
            jobOfferRepository.update(jobOfferId, updateJobOffer, companyId)
            val jobOffer = jobOfferRepository.findById(jobOfferId)
            call.respond(HttpStatusCode.OK, jobOffer)
        }

        delete("/{jobOfferId}") {
            val companyId = call.parameters.companyUUID
            val jobOfferId = call.parameters.jobOfferUUID
            jobOfferRepository.delete(jobOfferId, companyId)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}

@Suppress("LongMethod")
fun Route.publicCompanyJobOfferPromotionsRoutes() {
    val promotionRepository by inject<CompanyJobOfferPromotionRepository>()

    route("/companies/{companyId}/job-offers/{jobOfferId}/promotions") {
        get {
            val companyId = call.parameters.companyUUID
            val jobOfferId = call.parameters.jobOfferUUID
            val partnershipId = call.request.queryParameters["partnership_id"]?.toUUID()
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val pageSize = call.request.queryParameters["page_size"]?.toIntOrNull() ?: DEFAULT_PAGE_SIZE
            val promotions = promotionRepository.listJobOfferPromotions(
                companyId = companyId,
                jobOfferId = jobOfferId,
                partnershipId = partnershipId,
                page = page,
                pageSize = pageSize,
            )
            call.respond(HttpStatusCode.OK, promotions)
        }
    }

    route("/companies/{companyId}/partnerships/{partnershipId}/promote") {
        post {
            val companyId = call.parameters.companyUUID
            val partnershipId = call.parameters.partnershipId
            val request = call.receive<PromoteJobOfferRequest>(schema = "promote_job_offer.schema.json")
            val jobOfferId = request.jobOfferId.toUUID()

            val promotionId = promotionRepository.promoteJobOffer(
                companyId = companyId,
                partnershipId = partnershipId,
                jobOfferId = jobOfferId,
            )

            call.respond(HttpStatusCode.Created, mapOf("id" to promotionId.toString()))
        }
    }
}
