package fr.devlille.partners.connect.internal

import fr.devlille.partners.connect.integrations.domain.IntegrationProvider
import fr.devlille.partners.connect.integrations.domain.IntegrationUsage
import fr.devlille.partners.connect.integrations.infrastructure.db.BilletWebIntegrationsTable
import fr.devlille.partners.connect.integrations.infrastructure.db.IntegrationsTable
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

fun insertQontoIntegration(
    eventId: UUID = UUID.randomUUID(),
): UUID = transaction {
    IntegrationsTable.insertAndGetId {
        it[this.eventId] = eventId
        it[this.provider] = IntegrationProvider.QONTO
        it[this.usage] = IntegrationUsage.BILLING
    }.value
}

fun insertBilletWebIntegration(
    eventId: UUID = UUID.randomUUID(),
    basic: String = "valid-basic",
    bwEventId: String = UUID.randomUUID().toString(),
    rateId: String = UUID.randomUUID().toString(),
): UUID = transaction {
    val integrationId = IntegrationsTable.insertAndGetId {
        it[this.eventId] = eventId
        it[this.provider] = IntegrationProvider.BILLETWEB
        it[this.usage] = IntegrationUsage.TICKETING
    }
    BilletWebIntegrationsTable.insert {
        it[this.integrationId] = integrationId.value
        it[this.basic] = basic
        it[this.eventId] = bwEventId
        it[this.rateId] = rateId
    }
    integrationId.value
}
