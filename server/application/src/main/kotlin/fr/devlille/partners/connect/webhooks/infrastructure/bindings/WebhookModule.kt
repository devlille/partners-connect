package fr.devlille.partners.connect.webhooks.infrastructure.bindings

import fr.devlille.partners.connect.internal.infrastructure.bindings.getHttpClientEngine
import fr.devlille.partners.connect.internal.infrastructure.bindings.networkEngineModule
import fr.devlille.partners.connect.webhooks.application.WebhookRepositoryExposed
import fr.devlille.partners.connect.webhooks.domain.WebhookRepository
import fr.devlille.partners.connect.webhooks.infrastructure.gateways.HttpWebhookGateway
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.dsl.module

val webhookModule = module {
    includes(networkEngineModule)
    single<WebhookRepository> {
        val client = HttpClient(getHttpClientEngine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
        WebhookRepositoryExposed(listOf(HttpWebhookGateway(client)))
    }
}
