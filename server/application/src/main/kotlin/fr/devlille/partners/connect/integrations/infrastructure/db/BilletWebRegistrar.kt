package fr.devlille.partners.connect.integrations.infrastructure.db

import fr.devlille.partners.connect.integrations.domain.CreateIntegration
import fr.devlille.partners.connect.integrations.domain.IntegrationProvider
import fr.devlille.partners.connect.integrations.domain.IntegrationRegistrar
import fr.devlille.partners.connect.integrations.domain.IntegrationUsage
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

class BilletWebRegistrar : IntegrationRegistrar<CreateIntegration.CreateBilletWebIntegration> {
    override val supportedUsages = setOf(IntegrationUsage.TICKETING)

    override fun register(
        eventId: UUID,
        usage: IntegrationUsage,
        input: CreateIntegration.CreateBilletWebIntegration,
    ): UUID = transaction {
        val integrationId = IntegrationsTable.insertAndGetId {
            it[this.eventId] = eventId
            it[this.provider] = IntegrationProvider.BILLETWEB
            it[this.usage] = usage
        }
        BilletWebIntegrationsTable.insert {
            it[this.integrationId] = integrationId.value
            it[this.basic] = input.basic
            it[this.eventId] = input.eventId
            it[this.rateId] = input.rateId
        }
        integrationId.value
    }

    override fun unregister(integrationId: UUID): Unit = transaction {
        BilletWebIntegrationsTable.deleteWhere { BilletWebIntegrationsTable.integrationId eq integrationId }
        IntegrationsTable.deleteWhere { IntegrationsTable.id eq integrationId }
    }

    override fun supports(input: CreateIntegration): Boolean = input is CreateIntegration.CreateBilletWebIntegration
}
