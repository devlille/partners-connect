package fr.devlille.partners.connect.events.infrastructure.api

import fr.devlille.partners.connect.auth.domain.AuthRepository
import fr.devlille.partners.connect.events.domain.FavoriteEventRepository
import fr.devlille.partners.connect.internal.infrastructure.api.ConflictException
import fr.devlille.partners.connect.internal.infrastructure.api.token
import io.ktor.http.HttpStatusCode
import io.ktor.server.plugins.NotFoundException
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import org.koin.ktor.ext.inject

fun Route.favoriteEventRoutes() {
    val authRepository by inject<AuthRepository>()
    val favoriteRepository by inject<FavoriteEventRepository>()

    route("/users/me/favorite-events") {
        get {
            val userInfo = authRepository.getUserInfo(call.token)
            call.respond(HttpStatusCode.OK, favoriteRepository.listByUserEmail(userInfo.email))
        }
        put("/{eventSlug}") {
            val userInfo = authRepository.getUserInfo(call.token)
            val eventSlug = call.parameters.eventSlug
            val added = favoriteRepository.addFavorite(userInfo.email, eventSlug)
            if (!added) {
                throw ConflictException("Event $eventSlug is already in your favorites")
            }
            call.respond(HttpStatusCode.Created)
        }
        delete("/{eventSlug}") {
            val userInfo = authRepository.getUserInfo(call.token)
            val eventSlug = call.parameters.eventSlug
            val removed = favoriteRepository.removeFavorite(userInfo.email, eventSlug)
            if (!removed) {
                throw NotFoundException("Event $eventSlug is not in your favorites")
            }
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
