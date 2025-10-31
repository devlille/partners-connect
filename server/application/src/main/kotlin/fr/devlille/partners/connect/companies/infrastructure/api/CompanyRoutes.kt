package fr.devlille.partners.connect.companies.infrastructure.api

import fr.devlille.partners.connect.companies.domain.Company
import fr.devlille.partners.connect.companies.domain.CompanyImageProcessingRepository
import fr.devlille.partners.connect.companies.domain.CompanyJobOfferPromotionRepository
import fr.devlille.partners.connect.companies.domain.CompanyJobOfferRepository
import fr.devlille.partners.connect.companies.domain.CompanyMediaRepository
import fr.devlille.partners.connect.companies.domain.CompanyRepository
import fr.devlille.partners.connect.companies.domain.CompanyStatus
import fr.devlille.partners.connect.companies.domain.CreateCompany
import fr.devlille.partners.connect.companies.domain.CreateJobOffer
import fr.devlille.partners.connect.companies.domain.PromoteJobOfferRequest
import fr.devlille.partners.connect.companies.domain.UpdateCompany
import fr.devlille.partners.connect.companies.domain.UpdateJobOffer
import fr.devlille.partners.connect.events.domain.EventRepository
import fr.devlille.partners.connect.internal.infrastructure.api.DEFAULT_PAGE_SIZE
import fr.devlille.partners.connect.internal.infrastructure.api.UnsupportedMediaTypeException
import fr.devlille.partners.connect.internal.infrastructure.api.ValidationException
import fr.devlille.partners.connect.internal.infrastructure.ktor.asByteArray
import fr.devlille.partners.connect.internal.infrastructure.ktor.receive
import fr.devlille.partners.connect.internal.infrastructure.uuid.toUUID
import fr.devlille.partners.connect.notifications.domain.NotificationRepository
import fr.devlille.partners.connect.notifications.domain.NotificationVariables
import fr.devlille.partners.connect.partnership.domain.PartnershipRepository
import fr.devlille.partners.connect.partnership.infrastructure.api.partnershipId
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.MissingRequestParameterException
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.ktor.ext.inject

fun Route.companyRoutes() {
    val companyRepository by inject<CompanyRepository>()
    val imageProcessingRepository by inject<CompanyImageProcessingRepository>()
    val mediaRepository by inject<CompanyMediaRepository>()
    val partnershipRepository by inject<PartnershipRepository>()

    route("/companies") {
        companyCrudRoutes(companyRepository)
        companyLogoRoutes(companyRepository, imageProcessingRepository, mediaRepository)
        companyPartnershipRoutes(partnershipRepository)
        companyJobOfferRoutes()
        companyPromoteJobOfferRoute()
    }
}

private fun Route.companyCrudRoutes(companyRepository: CompanyRepository) {
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

private fun Route.companyLogoRoutes(
    companyRepository: CompanyRepository,
    imageProcessingRepository: CompanyImageProcessingRepository,
    mediaRepository: CompanyMediaRepository,
) {
    post("/{companyId}/logo") {
        val companyId = call.parameters.companyUUID
        val multipart = call.receiveMultipart()
        val part = multipart.readPart() ?: throw MissingRequestParameterException("file")
        val bytes = part.asByteArray()
        val mediaBinaries = when (part.contentType) {
            ContentType.Image.SVG -> imageProcessingRepository.processSvg(bytes)
            ContentType.Image.PNG, ContentType.Image.JPEG -> imageProcessingRepository.processImage(bytes)
            else -> throw UnsupportedMediaTypeException("Unsupported file type: ${part.contentType}")
        }
        val media = mediaRepository.upload(companyId.toString(), mediaBinaries)
        companyRepository.updateLogoUrls(companyId, media)
        call.respond(HttpStatusCode.OK, media)
    }
}

private fun Route.companyPartnershipRoutes(partnershipRepository: PartnershipRepository) {
    get("/{companyId}/partnership") {
        val companyId = call.parameters.companyUUID
        val items = partnershipRepository.listByCompany(companyId)
        call.respond(HttpStatusCode.OK, items)
    }
}

fun Route.companyPromoteJobOfferRoute() {
    val companyRepository by inject<CompanyRepository>()
    val partnershipRepository by inject<PartnershipRepository>()
    val notificationRepository by inject<NotificationRepository>()
    val eventRepository by inject<EventRepository>()
    val promotionRepository by inject<CompanyJobOfferPromotionRepository>()

    route("/{companyId}/partnerships/{partnershipId}/promote") {
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

            // Fetch the promotion to get complete data including eventSlug for notification
            val promotions = promotionRepository.listJobOfferPromotions(
                companyId = companyId,
                jobOfferId = jobOfferId,
                partnershipId = partnershipId,
                page = 1,
                pageSize = 1,
            )
            val promotion = promotions.items.firstOrNull()
                ?: throw NotFoundException("Promotion not found after creation")

            // Send notification to organizers
            val company = companyRepository.getById(companyId)
            val partnership = partnershipRepository.getById(promotion.eventSlug, partnershipId)
            val event = eventRepository.getBySlug(promotion.eventSlug)

            val variables = NotificationVariables.JobOfferPromoted(
                language = partnership.language,
                event = event,
                company = company,
                partnership = partnership,
                jobOffer = promotion.jobOffer,
            )
            notificationRepository.sendMessage(promotion.eventSlug, variables)

            call.respond(HttpStatusCode.Created, mapOf("id" to promotionId.toString()))
        }
    }
}

fun Route.companyJobOfferRoutes() {
    val jobOfferRepository by inject<CompanyJobOfferRepository>()
    val promotionRepository by inject<CompanyJobOfferPromotionRepository>()

    route("/{companyId}/job-offers") {
        post {
            val companyId = call.parameters.companyUUID
            val createJobOffer = call.receive<CreateJobOffer>(schema = "create_job_offer.schema.json")
            validatePublicationDate(createJobOffer.publicationDate)
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
            updateJobOffer.publicationDate?.let { validatePublicationDate(it) }
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

        get("/{jobOfferId}/promotions") {
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
}

/**
 * Validates publication date is not in the future.
 * @param publicationDate ISO format publication date
 * @throws ValidationException if date is in the future
 */
private fun validatePublicationDate(publicationDate: LocalDateTime) {
    val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
    if (publicationDate > now) {
        throw ValidationException("publication_date", "cannot be in the future")
    }
}
