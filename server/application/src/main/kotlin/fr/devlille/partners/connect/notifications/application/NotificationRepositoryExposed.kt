package fr.devlille.partners.connect.notifications.application

import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.events.infrastructure.db.findBySlug
import fr.devlille.partners.connect.integrations.domain.IntegrationUsage
import fr.devlille.partners.connect.integrations.infrastructure.db.IntegrationsTable
import fr.devlille.partners.connect.integrations.infrastructure.db.findByEventIdAndUsage
import fr.devlille.partners.connect.notifications.domain.NotificationGateway
import fr.devlille.partners.connect.notifications.domain.NotificationRepository
import fr.devlille.partners.connect.notifications.domain.NotificationVariables
import fr.devlille.partners.connect.notifications.infrastructure.gateways.WebhookService
import fr.devlille.partners.connect.webhooks.domain.WebhookGateway
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class NotificationRepositoryExposed(
    private val notificationGateways: List<NotificationGateway>,
    private val webhookGateway: WebhookGateway,
) : NotificationRepository {
    override fun sendMessage(eventSlug: String, variables: NotificationVariables): Unit = transaction {
        val eventEntity = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        val eventId = eventEntity.id.value

        IntegrationsTable
            .findByEventIdAndUsage(eventId, IntegrationUsage.NOTIFICATION)
            .forEach { row ->
                val provider = row[IntegrationsTable.provider]
                val integrationId = row[IntegrationsTable.id].value
                val gateway = notificationGateways.find { it.provider == provider }
                    ?: throw NotFoundException("No gateway for provider $provider")
                gateway.send(integrationId, variables)
            }

        val webhookService = WebhookService(webhookGateway)
        webhookService.sendWebhooks(eventId, variables)
    }
}
