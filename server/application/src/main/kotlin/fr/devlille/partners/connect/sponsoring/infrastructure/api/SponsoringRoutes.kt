package fr.devlille.partners.connect.sponsoring.infrastructure.api

import fr.devlille.partners.connect.internal.infrastructure.api.AuthorizedOrganisationPlugin
import fr.devlille.partners.connect.internal.infrastructure.uuid.toUUID
import fr.devlille.partners.connect.sponsoring.domain.AttachOptionsToPack
import fr.devlille.partners.connect.sponsoring.domain.CreateSponsoringOption
import fr.devlille.partners.connect.sponsoring.domain.CreateSponsoringPack
import fr.devlille.partners.connect.sponsoring.domain.OptionRepository
import fr.devlille.partners.connect.sponsoring.domain.PackRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

@Suppress("ThrowsCount")
fun Route.sponsoringRoutes() {
    route("/orgs/{orgSlug}/events/{eventId}") {
        packRoutes()
        optionRoutes()
    }
}

@Suppress("ThrowsCount")
private fun Route.packRoutes() {
    val repository by inject<PackRepository>()
    val optRepository by inject<OptionRepository>()

    route("/packs") {
        install(AuthorizedOrganisationPlugin)
        get {
            val eventId = call.parameters["eventId"]?.toUUID() ?: throw BadRequestException("Missing event id")
            val acceptLanguage = call.request.headers["Accept-Language"]
                ?.lowercase()
                ?: throw BadRequestException("Missing accept-language header")
            val packs = repository.findPacksByEvent(eventId = eventId, language = acceptLanguage)
            call.respond(HttpStatusCode.OK, packs)
        }
        post {
            val eventId = call.parameters["eventId"]?.toUUID() ?: throw BadRequestException("Missing event id")
            val request = call.receive<CreateSponsoringPack>()
            val packId = repository.createPack(eventId = eventId, input = request)
            call.respond(HttpStatusCode.Created, mapOf("id" to packId.toString()))
        }
        delete("/{packId}") {
            val eventId = call.parameters["eventId"]?.toUUID() ?: throw BadRequestException("Missing event id")
            val packId = call.parameters["packId"]?.toUUID() ?: throw BadRequestException("Missing pack id")
            repository.deletePack(eventId = eventId, packId = packId)
            call.respond(HttpStatusCode.NoContent)
        }
        post("/{packId}/options") {
            val eventId = call.parameters["eventId"]?.toUUID() ?: throw BadRequestException("Missing event id")
            val packId = call.parameters["packId"]?.toUUID() ?: throw BadRequestException("Missing pack id")
            val request = call.receive<AttachOptionsToPack>()
            optRepository.attachOptionsToPack(eventId = eventId, packId = packId, options = request)
            call.respond(HttpStatusCode.Created)
        }
        delete("/{packId}/options/{optionId}") {
            val eventId = call.parameters["eventId"]?.toUUID() ?: throw BadRequestException("Missing event id")
            val packId = call.parameters["packId"]?.toUUID() ?: throw BadRequestException("Missing pack id")
            val optionId = call.parameters["optionId"]?.toUUID() ?: throw BadRequestException("Missing option id")
            optRepository.detachOptionFromPack(eventId = eventId, packId = packId, optionId = optionId)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}

@Suppress("ThrowsCount")
private fun Route.optionRoutes() {
    val repository by inject<OptionRepository>()

    route("/options") {
        install(AuthorizedOrganisationPlugin)
        get {
            val eventId = call.parameters["eventId"]?.toUUID() ?: throw BadRequestException("Missing event id")
            val acceptLanguage = call.request.headers["Accept-Language"]
                ?.lowercase()
                ?: throw BadRequestException("Missing accept-language header")
            val options = repository.listOptionsByEvent(eventId = eventId, language = acceptLanguage)
            call.respond(HttpStatusCode.OK, options)
        }
        post {
            val eventId = call.parameters["eventId"]?.toUUID() ?: throw BadRequestException("Missing event id")
            val request = call.receive<CreateSponsoringOption>()
            val optionId = repository.createOption(eventId = eventId, input = request)
            call.respond(HttpStatusCode.Created, mapOf("id" to optionId.toString()))
        }
        put("/{optionId}") {
            val eventId = call.parameters["eventId"]?.toUUID() ?: throw BadRequestException("Missing event id")
            val optionId = call.parameters["optionId"]?.toUUID() ?: throw BadRequestException("Missing option id")
            val input = call.receive<CreateSponsoringOption>()
            val updatedId = repository.updateOption(eventId = eventId, optionId = optionId, input = input)
            call.respond(HttpStatusCode.OK, mapOf("id" to updatedId.toString()))
        }
        delete("/{optionId}") {
            val eventId = call.parameters["eventId"]?.toUUID() ?: throw BadRequestException("Missing event id")
            val optionId = call.parameters["optionId"]?.toUUID() ?: throw BadRequestException("Missing option id")
            repository.deleteOption(eventId = eventId, optionId = optionId)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
