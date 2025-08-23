package fr.devlille.partners.connect.webhooks.infrastructure.bindings

import fr.devlille.partners.connect.webhooks.domain.WebhookGateway
import fr.devlille.partners.connect.webhooks.infrastructure.gateways.HttpWebhookGateway
import org.koin.dsl.module

val webhookModule = module {
    single<WebhookGateway> { HttpWebhookGateway(get()) }
}
