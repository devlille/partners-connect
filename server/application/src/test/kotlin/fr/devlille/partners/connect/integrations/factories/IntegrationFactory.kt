package fr.devlille.partners.connect.integrations.factories

import fr.devlille.partners.connect.integrations.domain.IntegrationProvider
import fr.devlille.partners.connect.integrations.domain.IntegrationUsage
import fr.devlille.partners.connect.integrations.infrastructure.db.IntegrationEntity
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

fun insertMockedIntegration(
    id: UUID = UUID.randomUUID(),
    eventId: UUID,
    provider: IntegrationProvider = IntegrationProvider.SLACK,
    usage: IntegrationUsage = IntegrationUsage.NOTIFICATION,
): UUID = transaction {
    val integrationEntity = IntegrationEntity.new(id) {
        this.eventId = eventId
        this.provider = provider
        this.usage = usage
        this.createdAt = Clock.System.now().toLocalDateTime(TimeZone.UTC)
    }
    integrationEntity.id.value
}
