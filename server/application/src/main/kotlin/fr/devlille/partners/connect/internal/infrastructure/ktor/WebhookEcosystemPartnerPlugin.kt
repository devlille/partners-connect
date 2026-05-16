package fr.devlille.partners.connect.internal.infrastructure.ktor

import fr.devlille.partners.connect.ecosystempartners.infrastructure.api.ecosystemPartnerId
import fr.devlille.partners.connect.events.infrastructure.api.eventSlug
import fr.devlille.partners.connect.webhooks.domain.WebhookEventType
import fr.devlille.partners.connect.webhooks.domain.WebhookRepository
import fr.devlille.partners.connect.webhooks.domain.WebhookResourceType
import io.ktor.server.application.createRouteScopedPlugin
import org.koin.ktor.ext.inject

val WebhookEcosystemPartnerPlugin = createRouteScopedPlugin(name = "WebhookEcosystemPartnerPlugin") {
    val webhookRepository by application.inject<WebhookRepository>()

    onCallRespond { call ->
        val eventSlug = call.parameters.eventSlug
        val ecosystemPartnerId = call.parameters.ecosystemPartnerId
        webhookRepository.sendWebhooks(
            eventSlug = eventSlug,
            resourceType = WebhookResourceType.ECOSYSTEM_PARTNER,
            resourceId = ecosystemPartnerId,
            eventType = WebhookEventType.UPDATED,
        )
    }
}
