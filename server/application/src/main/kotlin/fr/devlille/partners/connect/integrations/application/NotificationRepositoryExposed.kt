package fr.devlille.partners.connect.integrations.application

import fr.devlille.partners.connect.integrations.domain.CreateIntegration
import fr.devlille.partners.connect.integrations.domain.CreateSlackIntegration
import fr.devlille.partners.connect.integrations.domain.IntegrationProvider
import fr.devlille.partners.connect.integrations.domain.IntegrationUsage
import fr.devlille.partners.connect.integrations.domain.NotificationGateway
import fr.devlille.partners.connect.integrations.domain.NotificationRepository
import fr.devlille.partners.connect.integrations.infrastructure.db.IntegrationsTable
import fr.devlille.partners.connect.integrations.infrastructure.db.SlackIntegrationsTable
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

class NotificationRepositoryExposed(
    private val gateways: List<NotificationGateway>,
) : NotificationRepository {
    override fun sendMessage(eventId: String, message: String) = transaction {
        val eventUUID = UUID.fromString(eventId)
        IntegrationsTable
            .selectAll()
            .where {
                (IntegrationsTable.eventId eq eventUUID) and (IntegrationsTable.usage eq IntegrationUsage.NOTIFICATION)
            }
            .forEach { row ->
                val provider = row[IntegrationsTable.provider]
                val integrationId = row[IntegrationsTable.id].value
                val gateway = gateways.find { it.provider == provider }
                    ?: throw NotFoundException("No gateway for provider $provider")
                gateway.send(integrationId, message)
            }
    }

    override fun register(eventId: String, input: CreateIntegration): String {
        return when (input) {
            is CreateSlackIntegration -> registerSlackIntegration(eventId, input)
            else -> throw IllegalArgumentException("Unsupported integration type: ${input::class.simpleName}")
        }
    }

    private fun registerSlackIntegration(eventId: String, input: CreateSlackIntegration): String = transaction {
        val integrationId = IntegrationsTable.insertAndGetId {
            it[this.eventId] = UUID.fromString(eventId)
            it[this.provider] = IntegrationProvider.SLACK
            it[this.usage] = IntegrationUsage.NOTIFICATION
        }

        SlackIntegrationsTable.insert {
            it[this.integrationId] = integrationId.value
            it[this.token] = input.token
            it[this.channel] = input.channel
        }

        integrationId.value.toString()
    }
}
