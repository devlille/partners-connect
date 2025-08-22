package fr.devlille.partners.connect.provider.infrastructure.api

import fr.devlille.partners.connect.auth.domain.AuthRepository
import fr.devlille.partners.connect.events.domain.EventRepository
import fr.devlille.partners.connect.internal.infrastructure.api.ForbiddenException
import fr.devlille.partners.connect.internal.infrastructure.api.UnauthorizedException
import fr.devlille.partners.connect.internal.infrastructure.api.token
import fr.devlille.partners.connect.provider.domain.CreateProvider
import fr.devlille.partners.connect.provider.domain.ProviderRepository
import io.ktor.server.plugins.NotFoundException
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

@Suppress("ThrowsCount")
fun Route.providerRoutes() {
    val providerRepository by inject<ProviderRepository>()
    val authRepository by inject<AuthRepository>()
    val eventRepository by inject<EventRepository>()

    route("/providers") {
        get {
            val query = call.request.queryParameters["query"]
            val sort = call.request.queryParameters["sort"]
            val direction = call.request.queryParameters["direction"]

            val providers = providerRepository.list(query, sort, direction)
            call.respond(HttpStatusCode.OK, providers)
        }

        post {
            // Requires authentication and user must be organizer of at least one event
            val token = call.token
            val userInfo = authRepository.getUserInfo(token)

            // Check if user is organizer (has at least one event)
            try {
                val userEvents = eventRepository.findByUserEmail(userInfo.email)
                if (userEvents.isEmpty()) {
                    throw ForbiddenException("You must be an organizer of at least one event to create providers")
                }
            } catch (e: NotFoundException) {
                // User not found in database
                throw ForbiddenException("You must be an organizer of at least one event to create providers")
            } catch (e: UnauthorizedException) {
                // User has no organizer permissions
                throw ForbiddenException("You must be an organizer of at least one event to create providers")
            }

            val input = call.receive<CreateProvider>()
            val providerId = providerRepository.create(input)
            call.respond(HttpStatusCode.Created, mapOf("id" to providerId.toString()))
        }
    }
}
