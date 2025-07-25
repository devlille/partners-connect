package fr.devlille.partners.connect.notifications.application

import fr.devlille.partners.connect.integrations.domain.IntegrationUsage
import fr.devlille.partners.connect.integrations.infrastructure.db.IntegrationsTable
import fr.devlille.partners.connect.integrations.infrastructure.db.findByEventIdAndUsage
import fr.devlille.partners.connect.notifications.domain.NotificationGateway
import fr.devlille.partners.connect.notifications.domain.NotificationRepository
import fr.devlille.partners.connect.notifications.domain.NotificationVariables
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

class NotificationRepositoryExposed(
    private val notificationGateways: List<NotificationGateway>,
) : NotificationRepository {
    override fun sendMessage(eventId: UUID, variables: NotificationVariables) = transaction {
        IntegrationsTable
            .findByEventIdAndUsage(eventId, IntegrationUsage.NOTIFICATION)
            .forEach { row ->
                val provider = row[IntegrationsTable.provider]
                val integrationId = row[IntegrationsTable.id].value
                val gateway = notificationGateways.find { it.provider == provider }
                    ?: throw NotFoundException("No gateway for provider $provider")
                gateway.send(integrationId, variables)
            }
    }
}
