package fr.devlille.partners.connect.events.infrastructure.api

import fr.devlille.partners.connect.events.domain.EventBudgetRepository
import fr.devlille.partners.connect.internal.infrastructure.ktor.AuthorizedOrganisationPlugin
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

fun Route.eventBudgetRoutes() {
    val repository by inject<EventBudgetRepository>()

    route("/orgs/{orgSlug}/events/{eventSlug}/budget") {
        install(AuthorizedOrganisationPlugin)

        get {
            val eventSlug = call.parameters.eventSlug
            val budget = repository.findByEventSlug(eventSlug)
            call.respond(HttpStatusCode.OK, budget)
        }
    }
}
