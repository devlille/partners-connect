package fr.devlille.partners.connect.webhooks.application

import fr.devlille.partners.connect.notifications.domain.NotificationVariables
import fr.devlille.partners.connect.webhooks.domain.EventWebhookData
import fr.devlille.partners.connect.webhooks.domain.PartnershipWebhookData
import fr.devlille.partners.connect.webhooks.domain.WebhookEventType
import fr.devlille.partners.connect.webhooks.domain.WebhookGateway
import fr.devlille.partners.connect.webhooks.domain.WebhookPayload
import kotlinx.coroutines.runBlocking
import java.util.UUID

class WebhookNotificationService(
    private val webhookGateways: List<WebhookGateway>,
) {
    fun sendWebhooks(eventId: UUID, variables: NotificationVariables): Boolean = runBlocking {
        val payload = createWebhookPayload(variables, eventId)

        var allSuccessful = true
        for (gateway in webhookGateways) {
            val success = gateway.sendWebhooks(eventId, payload)
            if (!success) {
                allSuccessful = false
            }
        }
        allSuccessful
    }

    private fun createWebhookPayload(variables: NotificationVariables, eventId: UUID): WebhookPayload {
        val eventType = when (variables) {
            is NotificationVariables.NewPartnership -> WebhookEventType.CREATED
            is NotificationVariables.PartnershipValidated -> WebhookEventType.UPDATED
            is NotificationVariables.PartnershipDeclined -> WebhookEventType.DELETED
            else -> WebhookEventType.UPDATED
        }

        val partnershipData = when (variables) {
            is NotificationVariables.NewPartnership -> PartnershipWebhookData(
                id = variables.partnership.id,
                companyId = variables.company.id,
                packId = variables.pack.id,
                status = "pending",
            )
            is NotificationVariables.PartnershipValidated -> PartnershipWebhookData(
                id = variables.partnership.id,
                companyId = variables.company.id,
                packId = variables.pack.id,
                status = "validated",
            )
            is NotificationVariables.PartnershipDeclined -> PartnershipWebhookData(
                id = variables.partnership.id,
                companyId = variables.company.id,
                packId = null,
                status = "declined",
            )
            else -> PartnershipWebhookData(
                id = "",
                companyId = variables.company.id,
                packId = null,
                status = "unknown",
            )
        }

        return WebhookPayload(
            eventType = eventType,
            partnership = partnershipData,
            event = EventWebhookData(
                id = eventId.toString(),
                slug = variables.event.event.slug,
                name = variables.event.event.name,
            ),
            timestamp = kotlinx.datetime.Clock.System.now().toString(),
        )
    }
}
