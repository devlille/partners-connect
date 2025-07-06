package fr.devlille.partners.connect.auth.infrastructure.api

import fr.devlille.partners.connect.auth.domain.AuthRepository
import fr.devlille.partners.connect.auth.infrastructure.api.mappers.toDomain
import fr.devlille.partners.connect.auth.infrastructure.api.mappers.toResponse
import fr.devlille.partners.connect.internal.infrastructure.api.UserSession
import fr.devlille.partners.connect.internal.infrastructure.api.token
import fr.devlille.partners.connect.users.domain.UserRepository
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.OAuthAccessTokenResponse
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.sessions.clear
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set
import org.koin.ktor.ext.inject

fun Route.authRoutes(callback: (state: String) -> String?) {
    val authRepository by inject<AuthRepository>()
    val userRepository by inject<UserRepository>()

    authenticate("google-oauth") {
        get("/login") {
            // Redirects to 'authorizeUrl' automatically
        }

        get("/callback") {
            val currentPrincipal: OAuthAccessTokenResponse.OAuth2? = call.principal()
            if (currentPrincipal == null || currentPrincipal.state == null) {
                call.respond(HttpStatusCode.NotFound)
                return@get
            }
            val state = currentPrincipal.state!!
            val session = UserSession(state, currentPrincipal.accessToken)
            call.sessions.set(session)
            val userInfo = authRepository.getUserInfo("Bearer ${session.token}")
            userRepository.createUser(userInfo.toDomain())
            val redirectUrl = callback(state)
            if (redirectUrl != null) {
                call.respondRedirect(redirectUrl)
            } else {
                call.respond(HttpStatusCode.OK, session)
            }
        }
    }

    get("/me") {
        call.respond(
            status = HttpStatusCode.OK,
            message = authRepository.getUserInfo(call.token).toResponse(),
        )
    }

    get("/logout") {
        call.sessions.clear<UserSession>()
        call.respond(HttpStatusCode.NoContent)
    }
}
