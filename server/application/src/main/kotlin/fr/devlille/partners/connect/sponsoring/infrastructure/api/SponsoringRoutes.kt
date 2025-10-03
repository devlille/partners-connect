package fr.devlille.partners.connect.sponsoring.infrastructure.api

import fr.devlille.partners.connect.events.infrastructure.api.eventSlug
import fr.devlille.partners.connect.internal.infrastructure.api.AuthorizedOrganisationPlugin
import fr.devlille.partners.connect.internal.infrastructure.api.MissingRequestHeaderException
import fr.devlille.partners.connect.internal.infrastructure.ktor.receive
import fr.devlille.partners.connect.sponsoring.domain.AttachOptionsToPack
import fr.devlille.partners.connect.sponsoring.domain.CreateSponsoringOption
import fr.devlille.partners.connect.sponsoring.domain.CreateSponsoringPack
import fr.devlille.partners.connect.sponsoring.domain.EventPackRepository
import fr.devlille.partners.connect.sponsoring.domain.OptionRepository
import fr.devlille.partners.connect.sponsoring.domain.PackRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

fun Route.sponsoringRoutes() {
    // Public routes (no authentication required)
    publicPackRoutes()

    // Authenticated organizational routes
    route("/orgs/{orgSlug}/events/{eventSlug}") {
        packRoutes()
        optionRoutes()
    }
}

private fun Route.packRoutes() {
    val repository by inject<PackRepository>()
    val optRepository by inject<OptionRepository>()

    route("/packs") {
        install(AuthorizedOrganisationPlugin)
        get {
            val eventSlug = call.parameters.eventSlug
            val acceptLanguage = call.request.headers["Accept-Language"]
                ?.lowercase()
                ?: throw MissingRequestHeaderException("accept-language")
            val packs = repository.findPacksByEvent(eventSlug = eventSlug, language = acceptLanguage)
            call.respond(HttpStatusCode.OK, packs)
        }
        post {
            val eventSlug = call.parameters.eventSlug
            val input = call.receive<CreateSponsoringPack>(schema = "create_sponsoring_pack.schema.json")
            val packId = repository.createPack(eventSlug, input)
            call.respond(HttpStatusCode.Created, mapOf("id" to packId.toString()))
        }
        delete("/{packId}") {
            val eventSlug = call.parameters.eventSlug
            val packId = call.parameters.packId
            repository.deletePack(eventSlug, packId)
            call.respond(HttpStatusCode.NoContent)
        }
        put("/{packId}") {
            val eventSlug = call.parameters.eventSlug
            val packId = call.parameters.packId
            val input = call.receive<CreateSponsoringPack>(schema = "create_sponsoring_pack.schema.json")
            val updatedId = repository.updatePack(eventSlug, packId, input)
            call.respond(HttpStatusCode.OK, mapOf("id" to updatedId.toString()))
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

private fun Route.optionRoutes() {
    val repository by inject<OptionRepository>()

    route("/options") {
        install(AuthorizedOrganisationPlugin)
        get {
            val eventSlug = call.parameters.eventSlug
            val acceptLanguage = call.request.headers["Accept-Language"]
                ?.lowercase()
                ?: throw MissingRequestHeaderException("accept-language")
            val options = repository.listOptionsByEvent(eventSlug = eventSlug, language = acceptLanguage)
            call.respond(HttpStatusCode.OK, options)
        }
        post {
            val eventSlug = call.parameters.eventSlug
            val request = call.receive<CreateSponsoringOption>(schema = "create_sponsoring_option.schema.json")
            val optionId = repository.createOption(eventSlug = eventSlug, input = request)
            call.respond(HttpStatusCode.Created, mapOf("id" to optionId.toString()))
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

/**
 * Public routes for sponsoring packages without authentication.
 *
 * These routes provide read-only access to sponsoring packages for public consumption,
 * allowing potential sponsors to view available packages and options without authentication.
 */
private fun Route.publicPackRoutes() {
    val eventPackRepository by inject<EventPackRepository>()

    route("/events/{eventSlug}/sponsoring/packs") {
        get {
            val eventSlug = call.parameters.eventSlug

            val acceptLanguage = call.request.headers["Accept-Language"]
                ?.lowercase()
                ?: throw MissingRequestHeaderException("accept-language")

            val packs = eventPackRepository.findPublicPacksByEvent(
                eventSlug = eventSlug,
                language = acceptLanguage,
            )
            call.respond(HttpStatusCode.OK, packs)
        }
    }
}
