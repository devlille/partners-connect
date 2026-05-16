package fr.devlille.partners.connect.sponsoring.infrastructure.api

import fr.devlille.partners.connect.events.infrastructure.api.eventSlug
import fr.devlille.partners.connect.internal.infrastructure.api.MissingRequestHeaderException
import fr.devlille.partners.connect.internal.infrastructure.api.UnsupportedMediaTypeException
import fr.devlille.partners.connect.internal.infrastructure.ktor.AuthorizedOrganisationPlugin
import fr.devlille.partners.connect.internal.infrastructure.ktor.asByteArray
import fr.devlille.partners.connect.internal.infrastructure.ktor.receive
import fr.devlille.partners.connect.sponsoring.domain.AttachOptionsToPack
import fr.devlille.partners.connect.sponsoring.domain.CreateSponsoringOption
import fr.devlille.partners.connect.sponsoring.domain.CreateSponsoringPack
import fr.devlille.partners.connect.sponsoring.domain.FlyerTemplateRepository
import fr.devlille.partners.connect.sponsoring.domain.FlyerZone
import fr.devlille.partners.connect.sponsoring.domain.OptionRepository
import fr.devlille.partners.connect.sponsoring.domain.PackRepository
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.MissingRequestParameterException
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import kotlinx.serialization.json.Json
import org.koin.ktor.ext.inject
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

fun Route.sponsoringRoutes() {
    // Public routes (no authentication required)
    publicPackRoutes()

    // Authenticated organizational routes
    orgsPackRoutes()
    orgsOptionRoutes()
}

/**
 * Public routes for sponsoring packages without authentication.
 *
 * These routes provide read-only access to sponsoring packages for public consumption,
 * allowing potential sponsors to view available packages and options without authentication.
 */
private fun Route.publicPackRoutes() {
    val eventPackRepository by inject<PackRepository>()

    route("/events/{eventSlug}/sponsoring/packs") {
        get {
            val eventSlug = call.parameters.eventSlug
            val acceptLanguage = call.request.headers["Accept-Language"]
                ?.lowercase()
                ?: throw MissingRequestHeaderException("accept-language")
            val packs = eventPackRepository.findPacksByEvent(eventSlug = eventSlug, language = acceptLanguage)
            call.respond(HttpStatusCode.OK, packs)
        }
    }
}

private fun Route.orgsPackRoutes() {
    val repository by inject<PackRepository>()
    val optRepository by inject<OptionRepository>()
    val flyerTemplateRepository by inject<FlyerTemplateRepository>()

    route("/orgs/{orgSlug}/events/{eventSlug}/packs") {
        install(AuthorizedOrganisationPlugin)
        get {
            val eventSlug = call.parameters.eventSlug
            val packs = repository.findPacksByEventWithAllTranslations(eventSlug = eventSlug)
            call.respond(HttpStatusCode.OK, packs)
        }
        post {
            val eventSlug = call.parameters.eventSlug
            val input = call.receive<CreateSponsoringPack>(schema = "create_sponsoring_pack.schema.json")
            val packId = repository.createPack(eventSlug, input)
            call.respond(HttpStatusCode.Created, mapOf("id" to packId.toString()))
        }
        put("/{packId}") {
            val eventSlug = call.parameters.eventSlug
            val packId = call.parameters.packId
            val input = call.receive<CreateSponsoringPack>(schema = "create_sponsoring_pack.schema.json")
            val updatedId = repository.updatePack(eventSlug, packId, input)
            call.respond(HttpStatusCode.OK, mapOf("id" to updatedId.toString()))
        }
        delete("/{packId}") {
            val eventSlug = call.parameters.eventSlug
            val packId = call.parameters.packId
            repository.deletePack(eventSlug, packId)
            call.respond(HttpStatusCode.NoContent)
        }
        put("/{packId}/flyer-template") {
            val eventSlug = call.parameters.eventSlug
            val packId = call.parameters.packId
            val (pngBytes, zone) = receiveFlyerTemplateUpload(call)
            val saved = flyerTemplateRepository.save(eventSlug, packId, pngBytes, zone)
            call.respond(HttpStatusCode.OK, mapOf("template_url" to saved.templateUrl))
        }
        delete("/{packId}/flyer-template") {
            val eventSlug = call.parameters.eventSlug
            val packId = call.parameters.packId
            flyerTemplateRepository.clear(eventSlug, packId)
            call.respond(HttpStatusCode.NoContent)
        }
        post("/{packId}/options") {
            val eventSlug = call.parameters.eventSlug
            val packId = call.parameters.packId
            val options = call.receive<AttachOptionsToPack>(schema = "attach_options_to_pack.schema.json")
            optRepository.attachOptionsToPack(eventSlug, packId, options)
            call.respond(HttpStatusCode.Created)
        }
        delete("/{packId}/options/{optionId}") {
            val eventSlug = call.parameters.eventSlug
            val packId = call.parameters.packId
            val optionId = call.parameters.optionId
            optRepository.detachOptionFromPack(eventSlug, packId, optionId)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}

private fun Route.orgsOptionRoutes() {
    val repository by inject<OptionRepository>()

    route("/orgs/{orgSlug}/events/{eventSlug}/options") {
        install(AuthorizedOrganisationPlugin)
        get {
            val eventSlug = call.parameters.eventSlug
            val options = repository.listOptionsWithPartnershipCounts(eventSlug = eventSlug)
            call.respond(HttpStatusCode.OK, options)
        }
        post {
            val eventSlug = call.parameters.eventSlug
            val request = call.receive<CreateSponsoringOption>(schema = "create_sponsoring_option.schema.json")
            val optionId = repository.createOption(eventSlug = eventSlug, input = request)
            call.respond(HttpStatusCode.Created, mapOf("id" to optionId.toString()))
        }
        get("/{optionId}") {
            val eventSlug = call.parameters.eventSlug
            val optionId = call.parameters.optionId
            val option = repository.getOptionByIdWithPartners(eventSlug = eventSlug, optionId = optionId)
            call.respond(HttpStatusCode.OK, option)
        }
        put("/{optionId}") {
            val eventSlug = call.parameters.eventSlug
            val optionId = call.parameters.optionId
            val input = call.receive<CreateSponsoringOption>(schema = "create_sponsoring_option.schema.json")
            val updatedId = repository.updateOption(eventSlug = eventSlug, optionId = optionId, input = input)
            call.respond(HttpStatusCode.OK, mapOf("id" to updatedId.toString()))
        }
        delete("/{optionId}") {
            val eventSlug = call.parameters.eventSlug
            val optionId = call.parameters.optionId
            repository.deleteOption(eventSlug = eventSlug, optionId = optionId)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}

private suspend fun receiveFlyerTemplateUpload(call: ApplicationCall): Pair<ByteArray, FlyerZone> {
    val multipart = call.receiveMultipart()
    var pngBytes: ByteArray? = null
    var zoneJson: String? = null
    multipart.forEachPart { part ->
        when (part) {
            is PartData.FileItem -> {
                val mime = part.contentType?.toString()
                if (mime != "image/png") {
                    part.dispose()
                    throw UnsupportedMediaTypeException("Unsupported template type: $mime — PNG only")
                }
                pngBytes = part.asByteArray()
            }
            is PartData.FormItem -> {
                if (part.name == "zone") zoneJson = part.value
                part.dispose()
            }
            else -> part.dispose()
        }
    }
    val bytes = pngBytes ?: throw MissingRequestParameterException("file")
    val raw = zoneJson ?: throw MissingRequestParameterException("zone")
    val zone = Json.decodeFromString(FlyerZone.serializer(), raw)
    validatePngFitsZone(bytes, zone)
    return bytes to zone
}

private fun validatePngFitsZone(pngBytes: ByteArray, zone: FlyerZone) {
    val image = ImageIO.read(ByteArrayInputStream(pngBytes))
        ?: throw BadRequestException("Uploaded file is not a readable PNG image")
    if (zone.x < 0 || zone.y < 0 || zone.width <= 0 || zone.height <= 0) {
        throw BadRequestException("Zone coordinates must be non-negative with positive width/height")
    }
    if (zone.x + zone.width > image.width || zone.y + zone.height > image.height) {
        throw BadRequestException(
            "Zone ($zone) does not fit inside template (${image.width}x${image.height})",
        )
    }
}
