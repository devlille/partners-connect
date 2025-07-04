package fr.devlille.partners.connect.auth.infrastructure.api

import fr.devlille.partners.connect.auth.domain.UserRepository
import fr.devlille.partners.connect.auth.infrastructure.api.mappers.toResponse
import fr.devlille.partners.connect.auth.infrastructure.providers.GoogleProvider
import io.ktor.client.HttpClient
import io.ktor.client.engine.java.Java
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.auth.OAuthAccessTokenResponse
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.sessions.clear
import io.ktor.server.sessions.get
import io.ktor.server.sessions.sessions
import io.ktor.server.sessions.set
import kotlinx.serialization.json.Json
import org.koin.ktor.ext.inject

fun Route.authRoutes(callback: (state: String) -> String?) {
    val repository by inject<UserRepository>()

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
            val redirectUrl = callback(state)
            if (redirectUrl != null) {
                call.respondRedirect(redirectUrl)
            } else {
                call.respond(HttpStatusCode.OK, session)
            }
        }
    }

    get("/me") {
        val token = call.request.headers["Authorization"]
            ?: call.sessions.get<UserSession>()?.let { "Bearer ${it.token}" }
        if (token == null) {
            call.respond(HttpStatusCode.Unauthorized)
            return@get
        }
        call.respond(HttpStatusCode.OK, repository.getUserInfo(token).toResponse())
    }

    get("/logout") {
        call.sessions.clear<UserSession>()
        call.respond(HttpStatusCode.NoContent)
    }
}
