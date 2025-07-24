package fr.devlille.partners.connect.notifications.application

import fr.devlille.partners.connect.integrations.domain.IntegrationUsage
import fr.devlille.partners.connect.integrations.infrastructure.db.IntegrationsTable
import fr.devlille.partners.connect.notifications.domain.NotificationGateway
import fr.devlille.partners.connect.notifications.domain.NotificationRepository
import fr.devlille.partners.connect.notifications.domain.NotificationVariables
import fr.devlille.partners.connect.notifications.domain.TemplateGateway
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

class NotificationRepositoryExposed(
    private val notificationGateways: List<NotificationGateway>,
    private val templateGateways: List<TemplateGateway>,
) : NotificationRepository {
    override fun sendMessage(eventId: String, variables: NotificationVariables) = transaction {
        val eventUUID = UUID.fromString(eventId)
        IntegrationsTable
            .selectAll()
            .where {
                (IntegrationsTable.eventId eq eventUUID) and (IntegrationsTable.usage eq IntegrationUsage.NOTIFICATION)
            }
            .forEach { row ->
                val provider = row[IntegrationsTable.provider]
                val integrationId = row[IntegrationsTable.id].value
                val gateway = notificationGateways.find { it.provider == provider }
                    ?: throw NotFoundException("No gateway for provider $provider")
                val content = templateGateways
                    .find { it.provider == provider }
                    ?.render(variables)
                    ?: throw NotFoundException("No template for provider $provider")
                gateway.send(integrationId, content)
            }
    }
}
