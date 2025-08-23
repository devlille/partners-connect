package fr.devlille.partners.connect.integrations.infrastructure.db

import fr.devlille.partners.connect.integrations.domain.CreateIntegration
import fr.devlille.partners.connect.integrations.domain.IntegrationProvider
import fr.devlille.partners.connect.integrations.domain.IntegrationRegistrar
import fr.devlille.partners.connect.integrations.domain.IntegrationUsage
import fr.devlille.partners.connect.integrations.domain.WebhookType
import fr.devlille.partners.connect.internal.infrastructure.uuid.toUUID
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEntity
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

class WebhookRegistrar : IntegrationRegistrar<CreateIntegration.CreateWebhookIntegration> {
    override val supportedUsages = setOf(IntegrationUsage.NOTIFICATION)

    override fun register(
        eventId: UUID,
        usage: IntegrationUsage,
        input: CreateIntegration.CreateWebhookIntegration,
    ): UUID = transaction {
        // Validate URL is not empty
        if (input.url.isBlank()) {
            throw BadRequestException("Webhook URL cannot be empty")
        }

        // If type is PARTNERSHIP, validate partnershipId is provided and exists
        var partnershipUuid: UUID? = null
        if (input.type == WebhookType.PARTNERSHIP) {
            if (input.partnershipId.isNullOrBlank()) {
                throw BadRequestException("Partnership ID is required for PARTNERSHIP type webhook")
            }
            partnershipUuid = input.partnershipId.toUUID()
            // Verify partnership exists
            PartnershipEntity.findById(partnershipUuid)
                ?: throw NotFoundException("Partnership not found: ${input.partnershipId}")
        }

        val integrationId = IntegrationsTable.insertAndGetId {
            it[this.eventId] = eventId
            it[this.provider] = IntegrationProvider.WEBHOOK
            it[this.usage] = usage
        }

        WebhookIntegrationsTable.insert {
            it[this.integrationId] = integrationId.value
            it[this.url] = input.url
            it[this.headerAuth] = input.headerAuth
            it[this.type] = input.type
            it[this.partnershipId] = partnershipUuid
        }

        integrationId.value
    }

    override fun supports(input: CreateIntegration): Boolean = input is CreateIntegration.CreateWebhookIntegration
}
