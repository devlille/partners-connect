package fr.devlille.partners.connect.sponsoring.infrastructure.api

import fr.devlille.partners.connect.internal.infrastructure.api.AuthorizedOrganisationPlugin
import fr.devlille.partners.connect.internal.infrastructure.api.BadRequestException
import fr.devlille.partners.connect.internal.infrastructure.uuid.toUUID
import fr.devlille.partners.connect.sponsoring.domain.AttachOptionsToPack
import fr.devlille.partners.connect.sponsoring.domain.CreateSponsoringOption
import fr.devlille.partners.connect.sponsoring.domain.CreateSponsoringPack
import fr.devlille.partners.connect.sponsoring.domain.OptionRepository
import fr.devlille.partners.connect.sponsoring.domain.PackRepository
import io.ktor.http.HttpStatusCode
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
    route("/orgs/{orgSlug}/events/{eventSlug}") {
        packRoutes()
        optionRoutes()
    }
}

@Suppress("ThrowsCount", "LongMethod")
private fun Route.packRoutes() {
    val repository by inject<PackRepository>()
    val optRepository by inject<OptionRepository>()

    route("/packs") {
        install(AuthorizedOrganisationPlugin)
        get {
            val eventSlug = call.parameters["eventSlug"] ?: throw BadRequestException(
                message = "Missing event slug",
            )
            val acceptLanguage = call.request.headers["Accept-Language"]
                ?.lowercase()
                ?: throw BadRequestException(
                    message = "Missing accept-language header",
                )
            val packs = repository.findPacksByEvent(eventSlug = eventSlug, language = acceptLanguage)
            call.respond(HttpStatusCode.OK, packs)
        }
        post {
            val eventSlug = call.parameters["eventSlug"] ?: throw BadRequestException(
                message = "Missing event slug",
            )
            val request = call.receive<CreateSponsoringPack>()
            val packId = repository.createPack(eventSlug = eventSlug, input = request)
            call.respond(HttpStatusCode.Created, mapOf("id" to packId.toString()))
        }
        delete("/{packId}") {
            val eventSlug = call.parameters["eventSlug"] ?: throw BadRequestException(
                message = "Missing event slug",
            )
            val packId = call.parameters["packId"]?.toUUID() ?: throw BadRequestException(
                message = "Missing pack id",
            )
            repository.deletePack(eventSlug = eventSlug, packId = packId)
            call.respond(HttpStatusCode.NoContent)
        }
        put("/{packId}") {
            val eventSlug = call.parameters["eventSlug"] ?: throw BadRequestException(
                message = "Missing event slug",
            )
            val packId = call.parameters["packId"]?.toUUID() ?: throw BadRequestException(
                message = "Missing pack id",
            )
            val input = call.receive<CreateSponsoringPack>()
            val updatedId = repository.updatePack(eventSlug = eventSlug, packId = packId, input = input)
            call.respond(HttpStatusCode.OK, mapOf("id" to updatedId.toString()))
        }
        post("/{packId}/options") {
            val eventSlug = call.parameters["eventSlug"] ?: throw BadRequestException(
                message = "Missing event slug",
            )
            val packId = call.parameters["packId"]?.toUUID() ?: throw BadRequestException(
                message = "Missing pack id",
            )
            val request = call.receive<AttachOptionsToPack>()
            optRepository.attachOptionsToPack(eventSlug = eventSlug, packId = packId, options = request)
            call.respond(HttpStatusCode.Created)
        }
        delete("/{packId}/options/{optionId}") {
            val eventSlug = call.parameters["eventSlug"] ?: throw BadRequestException(
                message = "Missing event slug",
            )
            val packId = call.parameters["packId"]?.toUUID() ?: throw BadRequestException(
                message = "Missing pack id",
            )
            val optionId = call.parameters["optionId"]?.toUUID() ?: throw BadRequestException(
                message = "Missing option id",
            )
            optRepository.detachOptionFromPack(eventSlug = eventSlug, packId = packId, optionId = optionId)
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
            val eventSlug = call.parameters["eventSlug"] ?: throw BadRequestException(
                message = "Missing event slug",
            )
            val acceptLanguage = call.request.headers["Accept-Language"]
                ?.lowercase()
                ?: throw BadRequestException(
                    message = "Missing accept-language header",
                )
            val options = repository.listOptionsByEvent(eventSlug = eventSlug, language = acceptLanguage)
            call.respond(HttpStatusCode.OK, options)
        }
        post {
            val eventSlug = call.parameters["eventSlug"] ?: throw BadRequestException(
                message = "Missing event slug",
            )
            val request = call.receive<CreateSponsoringOption>()
            val optionId = repository.createOption(eventSlug = eventSlug, input = request)
            call.respond(HttpStatusCode.Created, mapOf("id" to optionId.toString()))
        }
        put("/{optionId}") {
            val eventSlug = call.parameters["eventSlug"] ?: throw BadRequestException(
                message = "Missing event slug",
            )
            val optionId = call.parameters["optionId"]?.toUUID() ?: throw BadRequestException(
                message = "Missing option id",
            )
            val input = call.receive<CreateSponsoringOption>()
            val updatedId = repository.updateOption(eventSlug = eventSlug, optionId = optionId, input = input)
            call.respond(HttpStatusCode.OK, mapOf("id" to updatedId.toString()))
        }
        delete("/{optionId}") {
            val eventSlug = call.parameters["eventSlug"] ?: throw BadRequestException(
                message = "Missing event slug",
            )
            val optionId = call.parameters["optionId"]?.toUUID() ?: throw BadRequestException(
                message = "Missing option id",
            )
            repository.deleteOption(eventSlug = eventSlug, optionId = optionId)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
