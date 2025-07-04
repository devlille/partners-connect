package fr.devlille.partners.connect.auth.infrastructure.bindings

import fr.devlille.partners.connect.auth.application.GoogleUserRepository
import fr.devlille.partners.connect.auth.domain.UserRepository
import fr.devlille.partners.connect.auth.infrastructure.providers.GoogleProvider
import io.ktor.client.HttpClient
import io.ktor.client.engine.java.Java
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.dsl.module

val userModule = module {
    single<UserRepository> {
        val httpClient = HttpClient(Java) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
        GoogleUserRepository(GoogleProvider(httpClient))
    }
}
