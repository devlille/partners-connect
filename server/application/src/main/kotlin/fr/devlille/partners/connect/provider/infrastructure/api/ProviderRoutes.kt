package fr.devlille.partners.connect.provider.infrastructure.api

import fr.devlille.partners.connect.internal.infrastructure.api.AuthorizedOrganisationPlugin
import fr.devlille.partners.connect.internal.infrastructure.api.ConflictException
import fr.devlille.partners.connect.internal.infrastructure.api.DEFAULT_PAGE_SIZE
import fr.devlille.partners.connect.internal.infrastructure.ktor.receive
import fr.devlille.partners.connect.organisations.infrastructure.api.orgSlug
import fr.devlille.partners.connect.provider.domain.CreateProvider
import fr.devlille.partners.connect.provider.domain.ProviderRepository
import fr.devlille.partners.connect.provider.domain.UpdateProvider
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

fun Route.providersRoutes() {
    publicProvidersRoutes()

    orgsProvidersRoutes()
    orgsEventProviderRoutes()
}

fun Route.publicProvidersRoutes() {
    val providerRepository by inject<ProviderRepository>()

    route("/providers") {
        get {
            val orgSlug = call.request.queryParameters["org_slug"]
            val query = call.request.queryParameters["query"]
            val sort = call.request.queryParameters["sort"]
            val direction = call.request.queryParameters["direction"]
            val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
            val pageSize = call.request.queryParameters["page_size"]?.toIntOrNull() ?: DEFAULT_PAGE_SIZE

            val providers = providerRepository.list(orgSlug, query, sort, direction, page, pageSize)
            call.respond(HttpStatusCode.OK, providers)
        }
    }
}

fun Route.orgsProvidersRoutes() {
    val providerRepository by inject<ProviderRepository>()

    route("/orgs/{orgSlug}/providers") {
        install(AuthorizedOrganisationPlugin)

        post {
            val orgSlug = call.parameters.orgSlug
            val input = call.receive<CreateProvider>(schema = "create_provider.schema.json")
            val providerId = providerRepository.create(input, orgSlug)
            val provider = providerRepository.findByIdAndOrganisation(providerId, orgSlug)
            call.respond(HttpStatusCode.Created, provider)
        }

        route("/{providerId}") {
            put {
                val orgSlug = call.parameters.orgSlug
                val providerId = call.parameters.providerId
                val input = call.receive<UpdateProvider>(schema = "update_provider.schema.json")
                val updatedProvider = providerRepository.update(providerId, input, orgSlug)
                call.respond(HttpStatusCode.OK, updatedProvider)
            }

            delete {
                val orgSlug = call.parameters.orgSlug
                val providerId = call.parameters.providerId
                if (providerRepository.hasEventAttachments(providerId)) {
                    throw ConflictException("Provider is still attached to events and cannot be deleted")
                }
                val deleted = providerRepository.delete(providerId, orgSlug)
                if (!deleted) {
                    throw NotFoundException("Provider not found or not owned by organisation")
                }
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}
