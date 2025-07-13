package fr.devlille.partners.connect.sponsoring.infrastructure.api

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
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

@Suppress("ThrowsCount")
fun Route.sponsoringRoutes() {
    route("/events/{eventId}") {
        packRoutes()
        optionRoutes()
    }
}

@Suppress("ThrowsCount")
private fun Route.packRoutes() {
    val repository by inject<PackRepository>()
    val optRepository by inject<OptionRepository>()

    route("/packs") {
        get {
            val eventId = call.parameters["eventId"] ?: throw BadRequestException("Missing event id")
            val acceptLanguage = call.request.headers["Accept-Language"]
                ?.lowercase()
                ?: throw BadRequestException("Missing accept-language header")
            val packs = repository.findPacksByEvent(eventId, acceptLanguage)
            call.respond(HttpStatusCode.OK, packs)
        }
        post {
            val eventId = call.parameters["eventId"] ?: throw BadRequestException("Missing event id")
            val request = call.receive<CreateSponsoringPack>()
            val packId = repository.createPack(eventId, request)
            call.respond(HttpStatusCode.Created, SponsoringIdentifier(packId))
        }
        delete("/{packId}") {
            val eventId = call.parameters["eventId"] ?: throw BadRequestException("Missing event id")
            val packId = call.parameters["packId"] ?: throw BadRequestException("Missing pack id")
            repository.deletePack(eventId, packId)
            call.respond(HttpStatusCode.NoContent)
        }
        post("/{packId}/options") {
            val eventId = call.parameters["eventId"] ?: throw BadRequestException("Missing event id")
            val packId = call.parameters["packId"] ?: throw BadRequestException("Missing pack id")
            val request = call.receive<AttachOptionsToPack>()
            optRepository.attachOptionsToPack(eventId, packId, request)
            call.respond(HttpStatusCode.Created)
        }
        delete("/{packId}/options/{optionId}") {
            val eventId = call.parameters["eventId"] ?: throw BadRequestException("Missing event id")
            val packId = call.parameters["packId"] ?: throw BadRequestException("Missing pack id")
            val optionId = call.parameters["optionId"] ?: throw BadRequestException("Missing option id")
            optRepository.detachOptionFromPack(eventId, packId, optionId)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}

@Suppress("ThrowsCount")
private fun Route.optionRoutes() {
    val repository by inject<OptionRepository>()

    route("/options") {
        get {
            val eventId = call.parameters["eventId"] ?: throw BadRequestException("Missing event id")
            val acceptLanguage = call.request.headers["Accept-Language"]
                ?.lowercase()
                ?: throw BadRequestException("Missing accept-language header")
            val options = repository.listOptionsByEvent(eventId, acceptLanguage)
            call.respond(HttpStatusCode.OK, options)
        }
        post {
            val eventId = call.parameters["eventId"] ?: throw BadRequestException("Missing event id")
            val request = call.receive<CreateSponsoringOption>()
            val optionId = repository.createOption(eventId, request)
            call.respond(HttpStatusCode.Created, SponsoringIdentifier(optionId))
        }
        delete("/{optionId}") {
            val eventId = call.parameters["eventId"] ?: throw BadRequestException("Missing event id")
            val optionId = call.parameters["optionId"] ?: throw BadRequestException("Missing option id")
            repository.deleteOption(eventId, optionId)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
