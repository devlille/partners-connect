package fr.devlille.partners.connect.webhooks.infrastructure.bindings

import fr.devlille.partners.connect.webhooks.application.WebHookRepositoryExposed
import fr.devlille.partners.connect.webhooks.application.WebhookNotificationService
import fr.devlille.partners.connect.webhooks.domain.WebHookRepository
import fr.devlille.partners.connect.webhooks.domain.WebhookGateway
import fr.devlille.partners.connect.webhooks.infrastructure.gateways.WebhookGatewayImpl
import org.koin.dsl.module

val webhookModule = module {
    single<WebHookRepository> { WebHookRepositoryExposed() }
    single<WebhookGateway> { WebhookGatewayImpl(get()) }
    single { WebhookNotificationService(listOf(get<WebhookGateway>())) }
}
