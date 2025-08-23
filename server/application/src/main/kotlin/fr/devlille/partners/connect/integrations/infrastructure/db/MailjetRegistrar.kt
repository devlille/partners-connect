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

class MailjetRegistrar : IntegrationRegistrar<CreateIntegration.CreateMailjetIntegration> {
    override val supportedUsages = setOf(IntegrationUsage.NOTIFICATION)

    override fun register(
        eventId: UUID,
        usage: IntegrationUsage,
        input: CreateIntegration.CreateMailjetIntegration,
    ): UUID = transaction {
        val integrationId = IntegrationsTable.insertAndGetId {
            it[this.eventId] = eventId
            it[this.provider] = IntegrationProvider.MAILJET
            it[this.usage] = usage
        }
        MailjetIntegrationsTable.insert {
            it[this.integrationId] = integrationId.value
            it[this.apiKey] = input.apiKey
            it[this.secret] = input.secret
        }
        integrationId.value
    }

    override fun unregister(integrationId: UUID): Unit = transaction {
        MailjetIntegrationsTable.deleteWhere { MailjetIntegrationsTable.integrationId eq integrationId }
        IntegrationsTable.deleteWhere { IntegrationsTable.id eq integrationId }
    }

    override fun supports(input: CreateIntegration): Boolean = input is CreateIntegration.CreateMailjetIntegration
}
