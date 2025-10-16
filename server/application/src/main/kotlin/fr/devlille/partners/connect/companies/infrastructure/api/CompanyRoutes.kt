package fr.devlille.partners.connect.companies.infrastructure.api

import fr.devlille.partners.connect.companies.domain.CompanyImageProcessingRepository
import fr.devlille.partners.connect.companies.domain.CompanyJobOfferRepository
import fr.devlille.partners.connect.companies.domain.CompanyMediaRepository
import fr.devlille.partners.connect.companies.domain.CompanyRepository
import fr.devlille.partners.connect.companies.domain.CreateCompany
import fr.devlille.partners.connect.companies.domain.CreateJobOffer
import fr.devlille.partners.connect.companies.domain.UpdateJobOffer
import fr.devlille.partners.connect.internal.infrastructure.api.DEFAULT_PAGE_SIZE
import fr.devlille.partners.connect.internal.infrastructure.api.UnsupportedMediaTypeException
import fr.devlille.partners.connect.internal.infrastructure.api.ValidationException
import fr.devlille.partners.connect.internal.infrastructure.ktor.asByteArray
import fr.devlille.partners.connect.internal.infrastructure.ktor.receive
import fr.devlille.partners.connect.partnership.domain.PartnershipRepository
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
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

private const val MIN_EXPERIENCE_YEARS = 0
private const val MAX_EXPERIENCE_YEARS = 20

fun Route.companyRoutes() {
    val companyRepository by inject<CompanyRepository>()
    val imageProcessingRepository by inject<CompanyImageProcessingRepository>()
    val mediaRepository by inject<CompanyMediaRepository>()
    val partnershipRepository by inject<PartnershipRepository>()

    route("/companies") {
        get {
            val query = call.request.queryParameters["query"]?.trim()
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val pageSize = call.request.queryParameters["page_size"]?.toIntOrNull() ?: DEFAULT_PAGE_SIZE
            val companies = companyRepository.listPaginated(query, page, pageSize)
            call.respond(companies)
        }

        post {
            val input = call.receive<CreateCompany>(schema = "create_company.schema.json")
            val id = companyRepository.createOrUpdate(input)
            call.respond(HttpStatusCode.Created, mapOf("id" to id.toString()))
        }

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

        get("/{companyId}/partnership") {
            val companyId = call.parameters.companyUUID
            val items = partnershipRepository.listByCompany(companyId)
            call.respond(HttpStatusCode.OK, items)
        }

        // Job offer routes
        companyJobOfferRoutes()
    }
}

/**
 * Validates experience years field for job offers.
 * @param experienceYears The experience years to validate (nullable)
 * @throws ValidationException if years are outside valid range
 */
private fun validateExperienceYears(experienceYears: Int?) {
    experienceYears?.let { years ->
        if (years !in MIN_EXPERIENCE_YEARS..MAX_EXPERIENCE_YEARS) {
            throw ValidationException(
                "experience_years",
                "must be between $MIN_EXPERIENCE_YEARS and $MAX_EXPERIENCE_YEARS",
            )
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

/**
 * Checks if basic job offer fields are all null.
 */
private fun areBasicFieldsNull(updateJobOffer: UpdateJobOffer): Boolean =
    updateJobOffer.url == null && updateJobOffer.title == null && updateJobOffer.location == null

/**
 * Checks if date-related fields are all null.
 */
private fun areDateFieldsNull(updateJobOffer: UpdateJobOffer): Boolean =
    updateJobOffer.publicationDate == null && updateJobOffer.endDate == null

/**
 * Checks if additional job offer fields are all null.
 */
private fun areAdditionalFieldsNull(updateJobOffer: UpdateJobOffer): Boolean =
    updateJobOffer.experienceYears == null && updateJobOffer.salary == null

/**
 * Validates that at least one field is provided for update operations.
 * @param updateJobOffer The update request to validate
 * @throws ValidationException if all fields are null
 */
private fun validateUpdateNotEmpty(updateJobOffer: UpdateJobOffer) {
    if (areBasicFieldsNull(updateJobOffer) &&
        areDateFieldsNull(updateJobOffer) &&
        areAdditionalFieldsNull(updateJobOffer)
    ) {
        throw ValidationException("body", "at least one field must be provided for update")
    }
}

fun Route.companyJobOfferRoutes() {
    val jobOfferRepository by inject<CompanyJobOfferRepository>()

    route("/{companyId}/job-offers") {
        // POST /companies/{companyId}/job-offers - Create job offer
        post {
            val companyId = call.parameters.companyUUID
            val createJobOffer = call.receive<CreateJobOffer>(schema = "create_job_offer.schema.json")

            // Validate input data
            validateExperienceYears(createJobOffer.experienceYears)
            validatePublicationDate(createJobOffer.publicationDate)

            val jobOfferId = jobOfferRepository.create(companyId, createJobOffer)
            call.respond(HttpStatusCode.Created, mapOf("id" to jobOfferId.toString()))
        }

        // GET /companies/{companyId}/job-offers - List job offers with pagination
        get {
            val companyId = call.parameters.companyUUID
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val pageSize = call.request.queryParameters["page_size"]?.toIntOrNull() ?: DEFAULT_PAGE_SIZE
            val jobOffers = jobOfferRepository.findByCompany(companyId, page, pageSize)
            call.respond(HttpStatusCode.OK, jobOffers)
        }

        // GET /companies/{companyId}/job-offers/{jobOfferId} - Get single job offer
        get("/{jobOfferId}") {
            val companyId = call.parameters.companyUUID
            val jobOfferId = call.parameters.jobOfferUUID
            val jobOffer = jobOfferRepository.findById(jobOfferId)
            if (jobOffer.companyId != companyId.toString()) {
                throw NotFoundException("Job offer not found or not owned by company")
            }
            call.respond(HttpStatusCode.OK, jobOffer)
        }

        // PUT /companies/{companyId}/job-offers/{jobOfferId} - Update job offer
        put("/{jobOfferId}") {
            val companyId = call.parameters.companyUUID
            val jobOfferId = call.parameters.jobOfferUUID
            val updateJobOffer = call.receive<UpdateJobOffer>(schema = "update_job_offer.schema.json")

            // Validate input data
            validateUpdateNotEmpty(updateJobOffer)
            validateExperienceYears(updateJobOffer.experienceYears)
            updateJobOffer.publicationDate?.let { validatePublicationDate(it) }

            jobOfferRepository.update(jobOfferId, updateJobOffer, companyId)

            // Fetch the updated job offer to return
            val jobOffer = jobOfferRepository.findById(jobOfferId)
            call.respond(HttpStatusCode.OK, jobOffer)
        }

        // DELETE /companies/{companyId}/job-offers/{jobOfferId} - Delete job offer
        delete("/{jobOfferId}") {
            val companyId = call.parameters.companyUUID
            val jobOfferId = call.parameters.jobOfferUUID
            jobOfferRepository.delete(jobOfferId, companyId)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
