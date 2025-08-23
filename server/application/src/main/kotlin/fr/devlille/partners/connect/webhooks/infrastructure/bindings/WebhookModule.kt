package fr.devlille.partners.connect.webhooks.infrastructure.bindings

import fr.devlille.partners.connect.webhooks.application.WebhookRepositoryExposed
import fr.devlille.partners.connect.webhooks.domain.WebhookRepository
import fr.devlille.partners.connect.webhooks.infrastructure.gateways.HttpWebhookGateway
import org.koin.dsl.module

val webhookModule = module {
    single<WebhookRepository> { WebhookRepositoryExposed(listOf(HttpWebhookGateway(get()))) }
}
