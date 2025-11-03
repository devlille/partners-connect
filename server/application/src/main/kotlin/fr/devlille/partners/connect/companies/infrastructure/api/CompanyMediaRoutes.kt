package fr.devlille.partners.connect.companies.infrastructure.api

import fr.devlille.partners.connect.companies.domain.CompanyImageProcessingRepository
import fr.devlille.partners.connect.companies.domain.CompanyMediaRepository
import fr.devlille.partners.connect.companies.domain.CompanyRepository
import fr.devlille.partners.connect.internal.infrastructure.api.UnsupportedMediaTypeException
import fr.devlille.partners.connect.internal.infrastructure.ktor.asByteArray
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.MissingRequestParameterException
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import org.koin.ktor.ext.inject

fun Route.publicCompanyMediaRoutes() {
    val companyRepository by inject<CompanyRepository>()
    val imageProcessingRepository by inject<CompanyImageProcessingRepository>()
    val mediaRepository by inject<CompanyMediaRepository>()

    post("/companies/{companyId}/logo") {
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
