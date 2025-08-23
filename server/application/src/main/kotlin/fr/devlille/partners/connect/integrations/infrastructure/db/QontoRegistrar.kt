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

class QontoRegistrar : IntegrationRegistrar<CreateIntegration.CreateQontoIntegration> {
    override val supportedUsages = setOf(IntegrationUsage.BILLING)

    override fun register(
        eventId: UUID,
        usage: IntegrationUsage,
        input: CreateIntegration.CreateQontoIntegration,
    ): UUID = transaction {
        val integrationId = IntegrationsTable.insertAndGetId {
            it[this.eventId] = eventId
            it[this.provider] = IntegrationProvider.QONTO
            it[this.usage] = usage
        }
        QontoIntegrationsTable.insert {
            it[this.integrationId] = integrationId.value
            it[this.apiKey] = input.apiKey
            it[this.secret] = input.secret
            it[this.sandboxToken] = input.sandboxToken
        }
        integrationId.value
    }

    override fun unregister(integrationId: UUID): Unit = transaction {
        QontoIntegrationsTable.deleteWhere { QontoIntegrationsTable.integrationId eq integrationId }
        IntegrationsTable.deleteWhere { IntegrationsTable.id eq integrationId }
    }

    override fun supports(input: CreateIntegration): Boolean = input is CreateIntegration.CreateQontoIntegration
}
