package fr.devlille.partners.connect.auth.infrastructure.bindings

import fr.devlille.partners.connect.auth.application.AuthRepositoryGoogle
import fr.devlille.partners.connect.auth.domain.AuthRepository
import fr.devlille.partners.connect.auth.infrastructure.providers.GoogleProvider
import fr.devlille.partners.connect.internal.infrastructure.api.UnauthorizedException
import fr.devlille.partners.connect.internal.infrastructure.bindings.getHttpClientEngine
import io.ktor.client.HttpClient
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.dsl.module

val authModule = module {
    single<AuthRepository> {
        val httpClient = HttpClient(getHttpClientEngine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
            expectSuccess = true
            HttpResponseValidator {
                handleResponseExceptionWithRequest { exception, request ->
                    val clientException = exception as? ClientRequestException
                        ?: return@handleResponseExceptionWithRequest
                    val exceptionResponse = clientException.response
                    if (exceptionResponse.status == HttpStatusCode.Unauthorized) {
                        throw UnauthorizedException("Unauthorized: ${exception.message}")
                    }
                }
            }
        }
        AuthRepositoryGoogle(GoogleProvider(httpClient))
    }
}
