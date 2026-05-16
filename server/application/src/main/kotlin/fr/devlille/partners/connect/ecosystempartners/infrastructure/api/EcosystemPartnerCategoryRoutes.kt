package fr.devlille.partners.connect.ecosystempartners.infrastructure.api

import fr.devlille.partners.connect.ecosystempartners.domain.EcosystemPartnerCategoryRepository
import fr.devlille.partners.connect.ecosystempartners.domain.RegisterEcosystemPartnerCategory
import fr.devlille.partners.connect.ecosystempartners.domain.UpdateEcosystemPartnerCategory
import fr.devlille.partners.connect.events.infrastructure.api.eventSlug
import fr.devlille.partners.connect.internal.infrastructure.ktor.AuthorizedOrganisationPlugin
import fr.devlille.partners.connect.internal.infrastructure.ktor.receive
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

fun Route.ecosystemPartnerCategoryRoutes() {
    orgsEcosystemPartnerCategoryRoutes()
    publicEcosystemPartnerCategoryRoutes()
}

private fun Route.orgsEcosystemPartnerCategoryRoutes() {
    val repository by inject<EcosystemPartnerCategoryRepository>()

    route("/orgs/{orgSlug}/events/{eventSlug}/ecosystem-partner-categories") {
        install(AuthorizedOrganisationPlugin)

        post {
            val eventSlug = call.parameters.eventSlug
            val request = call.receive<RegisterEcosystemPartnerCategory>(
                schema = "register_ecosystem_partner_category.schema.json",
            )
            val id = repository.create(eventSlug, request)
            call.respond(HttpStatusCode.Created, mapOf("id" to id.toString()))
        }

        get {
            val eventSlug = call.parameters.eventSlug
            call.respond(HttpStatusCode.OK, repository.listByEvent(eventSlug))
        }
    }

    route("/orgs/{orgSlug}/events/{eventSlug}/ecosystem-partner-categories/{categoryId}") {
        install(AuthorizedOrganisationPlugin)

        put {
            val eventSlug = call.parameters.eventSlug
            val categoryId = call.parameters.ecosystemPartnerCategoryId
            val request = call.receive<UpdateEcosystemPartnerCategory>(
                schema = "update_ecosystem_partner_category.schema.json",
            )
            call.respond(HttpStatusCode.OK, repository.update(eventSlug, categoryId, request))
        }

        delete {
            val eventSlug = call.parameters.eventSlug
            val categoryId = call.parameters.ecosystemPartnerCategoryId
            repository.delete(eventSlug, categoryId)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}

private fun Route.publicEcosystemPartnerCategoryRoutes() {
    val repository by inject<EcosystemPartnerCategoryRepository>()

    route("/events/{eventSlug}/ecosystem-partner-categories") {
        get {
            val eventSlug = call.parameters.eventSlug
            call.respond(HttpStatusCode.OK, repository.listByEvent(eventSlug))
        }
    }
}
