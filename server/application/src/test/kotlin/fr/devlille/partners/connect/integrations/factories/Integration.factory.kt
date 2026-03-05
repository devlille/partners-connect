package fr.devlille.partners.connect.integrations.factories

import fr.devlille.partners.connect.integrations.domain.IntegrationProvider
import fr.devlille.partners.connect.integrations.domain.IntegrationUsage
import fr.devlille.partners.connect.integrations.infrastructure.db.IntegrationEntity
import fr.devlille.partners.connect.integrations.infrastructure.db.SlackIntegrationsTable
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.jdbc.insert
import java.util.UUID

fun insertMockedIntegration(
    id: UUID = UUID.randomUUID(),
    eventId: UUID,
    provider: IntegrationProvider = IntegrationProvider.SLACK,
    usage: IntegrationUsage = IntegrationUsage.NOTIFICATION,
): UUID {
    val integrationEntity = IntegrationEntity.new(id) {
        this.eventId = eventId
        this.provider = provider
        this.usage = usage
        this.createdAt = Clock.System.now().toLocalDateTime(TimeZone.UTC)
    }
    return integrationEntity.id.value
}

fun insertSlackIntegration(
    integrationId: UUID,
    channel: String = "#general",
    token: String = "xoxb-1234567890-token",
): UUID {
    SlackIntegrationsTable.insert {
        it[this.integrationId] = integrationId
        it[this.token] = token
        it[this.channel] = channel
    }
    return integrationId
}
