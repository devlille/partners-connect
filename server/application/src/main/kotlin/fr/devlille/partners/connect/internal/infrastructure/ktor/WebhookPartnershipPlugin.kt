package fr.devlille.partners.connect.internal.infrastructure.ktor

import fr.devlille.partners.connect.events.infrastructure.api.eventSlug
import fr.devlille.partners.connect.partnership.infrastructure.api.partnershipId
import fr.devlille.partners.connect.webhooks.domain.WebhookEventType
import fr.devlille.partners.connect.webhooks.domain.WebhookRepository
import fr.devlille.partners.connect.webhooks.domain.WebhookResourceType
import io.ktor.server.application.createRouteScopedPlugin
import org.koin.ktor.ext.inject

val WebhookPartnershipPlugin = createRouteScopedPlugin(name = "WebhookPartnershipPlugin") {
    val webhookRepository by application.inject<WebhookRepository>()

    onCallRespond { call ->
        val eventSlug = call.parameters.eventSlug
        val partnershipId = call.parameters.partnershipId
        webhookRepository.sendWebhooks(
            eventSlug = eventSlug,
            resourceType = WebhookResourceType.PARTNERSHIP,
            resourceId = partnershipId,
            eventType = WebhookEventType.UPDATED,
        )
    }
}
