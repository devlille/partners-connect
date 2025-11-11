package fr.devlille.partners.connect.events.infrastructure.api

import fr.devlille.partners.connect.agenda.domain.AgendaRepository
import fr.devlille.partners.connect.internal.infrastructure.api.AuthorizedOrganisationPlugin
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

fun Route.publicEventAgendaRoutes() {
    val agendaRepository by inject<AgendaRepository>()

    route("/events/{eventSlug}/agenda") {
        get {
            val eventSlug = call.parameters.eventSlug
            val agendaResponse = agendaRepository.getAgendaByEventSlug(eventSlug)
            call.respond(HttpStatusCode.OK, agendaResponse)
        }
    }
}

fun Route.orgsEventAgendaRoutes() {
    val agendaRepository by inject<AgendaRepository>()

    route("/orgs/{orgSlug}/events/{eventSlug}/agenda") {
        install(AuthorizedOrganisationPlugin)

        post {
            val eventSlug = call.parameters.eventSlug
            agendaRepository.fetchAndStore(eventSlug)
            call.respond(HttpStatusCode.Created)
        }
    }
}
