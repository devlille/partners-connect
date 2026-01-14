package fr.devlille.partners.connect.notifications.application

import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.events.infrastructure.db.findBySlug
import fr.devlille.partners.connect.integrations.domain.IntegrationUsage
import fr.devlille.partners.connect.integrations.infrastructure.db.IntegrationEntity
import fr.devlille.partners.connect.integrations.infrastructure.db.IntegrationsTable
import fr.devlille.partners.connect.notifications.domain.DeliveryResult
import fr.devlille.partners.connect.notifications.domain.Destination
import fr.devlille.partners.connect.notifications.domain.NotificationGateway
import fr.devlille.partners.connect.notifications.domain.NotificationRepository
import fr.devlille.partners.connect.notifications.domain.NotificationVariables
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class NotificationRepositoryExposed(
    private val notificationGateways: List<NotificationGateway>,
    private val mailingGateway: NotificationGateway,
) : NotificationRepository {
    override suspend fun sendMessage(variables: NotificationVariables): List<DeliveryResult> {
        val eventSlug = variables.event.event.slug
        val eventEntity = transaction {
            EventEntity.findBySlug(eventSlug)
                ?: throw NotFoundException("Event with slug $eventSlug not found")
        }
        val integrations = transaction {
            IntegrationEntity
                .find {
                    IntegrationsTable.eventId eq eventEntity.id.value and
                        (IntegrationsTable.usage eq IntegrationUsage.NOTIFICATION)
                }
                .toList()
        }

        // Send to all integrations and collect results
        val results = integrations.map { integration ->
            val gateway = notificationGateways.find { it.provider == integration.provider }
                ?: throw NotFoundException("No gateway for provider ${integration.provider}")
            gateway.send(integration.id.value, variables)
        }

        return results
    }

    override suspend fun sendMessage(
        eventSlug: String,
        destination: Destination,
        subject: String,
        htmlBody: String,
    ): DeliveryResult {
        val eventEntity = transaction {
            EventEntity.findBySlug(eventSlug)
                ?: throw NotFoundException("Event with slug $eventSlug not found")
        }

        // Find integration id for event
        val integration = transaction {
            IntegrationEntity.find {
                (IntegrationsTable.eventId eq eventEntity.id.value) and
                    (IntegrationsTable.provider eq mailingGateway.provider)
            }.firstOrNull()
        } ?: throw NotFoundException("No mailing integration found for event")

        // Call generic gateway send method and return result
        return mailingGateway.send(
            integrationId = integration.id.value,
            destination = destination,
            header = subject,
            body = htmlBody,
        )
    }
}
