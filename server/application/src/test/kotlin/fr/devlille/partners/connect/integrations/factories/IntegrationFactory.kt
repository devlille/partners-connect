package fr.devlille.partners.connect.integrations.factories

import fr.devlille.partners.connect.integrations.domain.IntegrationProvider
import fr.devlille.partners.connect.integrations.domain.IntegrationUsage
import fr.devlille.partners.connect.integrations.infrastructure.db.IntegrationsTable
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

fun insertMockedIntegration(
    id: UUID = UUID.randomUUID(),
    eventId: UUID,
    provider: IntegrationProvider = IntegrationProvider.SLACK,
    usage: IntegrationUsage = IntegrationUsage.NOTIFICATION,
): UUID = transaction {
    IntegrationsTable.insert {
        it[IntegrationsTable.id] = id
        it[IntegrationsTable.eventId] = eventId
        it[IntegrationsTable.provider] = provider
        it[IntegrationsTable.usage] = usage
        it[IntegrationsTable.createdAt] = Clock.System.now().toLocalDateTime(TimeZone.UTC)
    }
    id
}