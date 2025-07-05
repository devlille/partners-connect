package fr.devlille.partners.connect.auth.infrastructure.plugins

import fr.devlille.partners.connect.internal.infrastructure.system.SystemVarEnv
import io.ktor.client.HttpClient
import io.ktor.client.engine.java.Java
import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.OAuthServerSettings
import io.ktor.server.auth.oauth

fun Application.configureSecurity(redirect: (state: String, redirectUrl: String) -> Unit) {
    install(Authentication) {
        oauth("google-oauth") {
            urlProvider = { "http://localhost:8080/auth/callback" }
            providerLookup = {
                OAuthServerSettings.OAuth2ServerSettings(
                    name = "google",
                    authorizeUrl = "https://accounts.google.com/o/oauth2/auth",
                    accessTokenUrl = "https://accounts.google.com/o/oauth2/token",
                    requestMethod = HttpMethod.Post,
                    clientId = SystemVarEnv.GoogleProvider.clientId,
                    clientSecret = SystemVarEnv.GoogleProvider.clientSecret,
                    defaultScopes = listOf("openid", "profile", "email"),
                    extraAuthParameters = listOf("access_type" to "offline"),
                    onStateCreated = { call, state ->
                        //saves new state with redirect url value
                        call.request.queryParameters["redirectUrl"]?.let {
                            redirect(state, it)
                        }
                    }
                )
            }
            client = HttpClient(Java)
        }
    }
}
