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

class OpenPlannerRegistrar : IntegrationRegistrar<CreateIntegration.CreateOpenPlannerIntegration> {
    override val supportedUsages: Set<IntegrationUsage> = setOf(IntegrationUsage.AGENDA)

    override fun register(
        eventId: UUID,
        usage: IntegrationUsage,
        input: CreateIntegration.CreateOpenPlannerIntegration,
    ): UUID = transaction {
        val integrationId = IntegrationsTable.insertAndGetId {
            it[this.eventId] = eventId
            it[this.provider] = IntegrationProvider.OPENPLANNER
            it[this.usage] = usage
        }
        OpenPlannerIntegrationsTable.insert {
            it[this.integrationId] = integrationId.value
            it[this.eventId] = input.eventId
            it[this.apiKey] = input.apiKey
        }
        integrationId.value
    }

    override fun unregister(integrationId: UUID): Unit = transaction {
        OpenPlannerIntegrationsTable.deleteWhere { OpenPlannerIntegrationsTable.integrationId eq integrationId }
        IntegrationsTable.deleteWhere { IntegrationsTable.id eq integrationId }
    }

    override fun supports(input: CreateIntegration): Boolean = input is CreateIntegration.CreateOpenPlannerIntegration
}
