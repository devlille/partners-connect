package fr.devlille.partners.connect.internal.infrastructure.bindings

import fr.devlille.partners.connect.internal.infrastructure.api.UnauthorizedException
import io.ktor.client.HttpClient
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpResponseValidator
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.dsl.module

val networkClientModule = module {
    includes(networkEngineModule)
    single<HttpClient> {
        HttpClient(getHttpClientEngine) {
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
    }
}
