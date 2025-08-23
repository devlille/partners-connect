package fr.devlille.partners.connect.webhooks.infrastructure.bindings

import fr.devlille.partners.connect.webhooks.application.WebHookRepositoryExposed
import fr.devlille.partners.connect.webhooks.application.WebhookNotificationService
import fr.devlille.partners.connect.webhooks.domain.WebhookRepository
import fr.devlille.partners.connect.webhooks.domain.WebhookGateway
import fr.devlille.partners.connect.webhooks.infrastructure.gateways.HttpWebhookGateway
import org.koin.dsl.module

val webhookModule = module {
    single<WebhookRepository> { WebHookRepositoryExposed() }
    single<WebhookGateway> { HttpWebhookGateway(get()) }
    single { WebhookNotificationService(listOf(get<WebhookGateway>())) }
}
