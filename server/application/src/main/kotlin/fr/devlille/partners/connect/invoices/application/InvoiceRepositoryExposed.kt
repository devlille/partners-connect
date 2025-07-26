package fr.devlille.partners.connect.invoices.application

import fr.devlille.partners.connect.integrations.domain.IntegrationUsage
import fr.devlille.partners.connect.integrations.infrastructure.db.IntegrationsTable
import fr.devlille.partners.connect.integrations.infrastructure.db.findByEventIdAndUsage
import fr.devlille.partners.connect.invoices.domain.InvoiceGateway
import fr.devlille.partners.connect.invoices.domain.InvoiceRepository
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

class InvoiceRepositoryExposed(
    private val invoiceGateways: List<InvoiceGateway>,
) : InvoiceRepository {
    override fun createInvoice(eventId: UUID, companyId: UUID): String = transaction {
        val integrations = IntegrationsTable
            .findByEventIdAndUsage(eventId, IntegrationUsage.INVOICE)
            .toList()
        if (integrations.isEmpty()) {
            throw NotFoundException("No invoice integration found for event $eventId")
        }
        if (integrations.size > 1) {
            throw NotFoundException("Multiple invoice integrations found for event $eventId")
        }
        val integration = integrations.single()
        val provider = integration[IntegrationsTable.provider]
        val integrationId = integration[IntegrationsTable.id].value
        val gateway = invoiceGateways.find { it.provider == provider }
            ?: throw NotFoundException("No gateway for provider $provider")
        gateway.createInvoice(integrationId, eventId, companyId)
    }
}
