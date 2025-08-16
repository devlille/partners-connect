package fr.devlille.partners.connect.companies.infrastructure.api

import fr.devlille.partners.connect.companies.domain.CompanyImageProcessingRepository
import fr.devlille.partners.connect.companies.domain.CompanyMediaRepository
import fr.devlille.partners.connect.companies.domain.CompanyRepository
import fr.devlille.partners.connect.companies.domain.CreateCompany
import fr.devlille.partners.connect.internal.infrastructure.ktor.asByteArray
import fr.devlille.partners.connect.internal.infrastructure.uuid.toUUID
import fr.devlille.partners.connect.partnership.domain.PartnershipRepository
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.receive
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

@Suppress("ThrowsCount")
fun Route.companyRoutes() {
    val companyRepository by inject<CompanyRepository>()
    val imageProcessingRepository by inject<CompanyImageProcessingRepository>()
    val mediaRepository by inject<CompanyMediaRepository>()
    val partnershipRepository by inject<PartnershipRepository>()

    route("/companies") {
        get {
            val query = call.request.queryParameters["query"]?.trim()
            val companies = companyRepository.list(query)
            call.respond(companies)
        }

        post {
            val input = call.receive<CreateCompany>()
            val id = companyRepository.createOrUpdate(input)
            call.respond(HttpStatusCode.Created, mapOf("id" to id.toString()))
        }

        post("/{companyId}/logo") {
            val companyId = call.parameters["companyId"]?.toUUID() ?: throw BadRequestException("Missing company id")
            val multipart = call.receiveMultipart()
            val part = multipart.readPart() ?: throw BadRequestException("Missing file part")
            val bytes = part.asByteArray()
            val mediaBinaries = when (part.contentType) {
                ContentType.Image.SVG -> imageProcessingRepository.processSvg(bytes)
                ContentType.Image.PNG, ContentType.Image.JPEG -> imageProcessingRepository.processImage(bytes)
                else -> throw BadRequestException("Unsupported file type: ${part.contentType}")
            }
            val media = mediaRepository.upload(companyId.toString(), mediaBinaries)
            companyRepository.updateLogoUrls(companyId, media)
            call.respond(HttpStatusCode.OK, media)
        }

        get("/{companyId}/partnership") {
            val companyId = call.parameters["companyId"]?.toUUID() ?: throw BadRequestException("Missing company id")
            // Check if the company exists
            companyRepository.getById(companyId) // This will throw NotFoundException if not found
            val items = partnershipRepository.listByCompany(companyId)
            call.respond(HttpStatusCode.OK, items)
        }
    }
}
