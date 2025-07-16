package fr.devlille.partners.connect.integrations.infrastructure.db

import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

class SlackConfigDao {
    operator fun get(integrationId: UUID): SlackConfig = transaction {
        SlackIntegrationsTable
            .selectAll()
            .where { SlackIntegrationsTable.integrationId eq integrationId }
            .map {
                SlackConfig(
                    token = it[SlackIntegrationsTable.token],
                    channel = it[SlackIntegrationsTable.channel],
                )
            }
            .singleOrNull()
            ?: throw NotFoundException("Slack config not found")
    }
}

data class SlackConfig(val token: String, val channel: String)
