package fr.devlille.partners.connect.notifications.infrastructure.gateways

import fr.devlille.partners.connect.integrations.domain.IntegrationProvider
import fr.devlille.partners.connect.integrations.domain.IntegrationUsage
import fr.devlille.partners.connect.integrations.infrastructure.db.IntegrationsTable
import fr.devlille.partners.connect.integrations.infrastructure.db.findByEventIdAndUsage
import fr.devlille.partners.connect.notifications.domain.NotificationVariables
import fr.devlille.partners.connect.webhooks.domain.EventWebhookData
import fr.devlille.partners.connect.webhooks.domain.PartnershipWebhookData
import fr.devlille.partners.connect.webhooks.domain.WebhookEventType
import fr.devlille.partners.connect.webhooks.domain.WebhookGateway
import fr.devlille.partners.connect.webhooks.domain.WebhookPayload
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

class WebhookService(
    private val webhookGateway: WebhookGateway,
) {
    fun sendWebhooks(eventId: UUID, variables: NotificationVariables): Boolean = runBlocking {
        val webhookIntegrations = transaction {
            IntegrationsTable
                .findByEventIdAndUsage(eventId, IntegrationUsage.NOTIFICATION)
                .filter { it[IntegrationsTable.provider] == IntegrationProvider.WEBHOOK }
        }

        // Convert NotificationVariables to webhook payload
        val payload = createWebhookPayload(variables) ?: return@runBlocking true // No webhook payload needed

        var allSuccessful = true
        for (integration in webhookIntegrations) {
            val integrationId = integration[IntegrationsTable.id].value

            if (webhookGateway.canSendWebhook(eventId, integrationId)) {
                val success = webhookGateway.sendHttpCall(integrationId, payload)
                if (!success) {
                    allSuccessful = false
                }
            }
        }
        allSuccessful
    }

    private fun createWebhookPayload(variables: NotificationVariables): WebhookPayload? {
        return when (variables) {
            is NotificationVariables.NewPartnership -> {
                WebhookPayload(
                    eventType = WebhookEventType.CREATED,
                    partnership = PartnershipWebhookData(
                        id = variables.partnership.id,
                        companyId = variables.company.id,
                        packId = variables.pack.id,
                        status = "pending",
                    ),
                    event = EventWebhookData(
                        // Using slug as id for consistency
                        id = variables.event.event.slug,
                        slug = variables.event.event.slug,
                        name = variables.event.event.name,
                    ),
                    timestamp = Clock.System.now().toString(),
                )
            }
            is NotificationVariables.PartnershipValidated -> {
                WebhookPayload(
                    eventType = WebhookEventType.UPDATED,
                    partnership = PartnershipWebhookData(
                        id = variables.partnership.id,
                        companyId = variables.company.id,
                        packId = variables.pack.id,
                        status = "validated",
                    ),
                    event = EventWebhookData(
                        // Using slug as id for consistency
                        id = variables.event.event.slug,
                        slug = variables.event.event.slug,
                        name = variables.event.event.name,
                    ),
                    timestamp = Clock.System.now().toString(),
                )
            }
            is NotificationVariables.PartnershipDeclined -> {
                WebhookPayload(
                    eventType = WebhookEventType.DELETED,
                    partnership = PartnershipWebhookData(
                        id = variables.partnership.id,
                        companyId = variables.company.id,
                        // PartnershipDeclined doesn't have pack
                        packId = null,
                        status = "declined",
                    ),
                    event = EventWebhookData(
                        // Using slug as id for consistency
                        id = variables.event.event.slug,
                        slug = variables.event.event.slug,
                        name = variables.event.event.name,
                    ),
                    timestamp = Clock.System.now().toString(),
                )
            }
            // No webhook notification for other types
            else -> null
        }
    }
}
