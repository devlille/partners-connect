package fr.devlille.partners.connect.billing.infrastructure.gateways

import fr.devlille.partners.connect.billing.domain.BillingGateway
import fr.devlille.partners.connect.billing.infrastructure.gateways.models.QontoClient
import fr.devlille.partners.connect.billing.infrastructure.gateways.models.mappers.invoiceItems
import fr.devlille.partners.connect.billing.infrastructure.gateways.models.mappers.toQontoClientRequest
import fr.devlille.partners.connect.billing.infrastructure.gateways.models.mappers.toQontoInvoiceRequest
import fr.devlille.partners.connect.billing.infrastructure.gateways.models.mappers.toQontoQuoteRequest
import fr.devlille.partners.connect.billing.infrastructure.providers.QontoProvider
import fr.devlille.partners.connect.companies.infrastructure.db.CompanyEntity
import fr.devlille.partners.connect.integrations.domain.IntegrationProvider
import fr.devlille.partners.connect.integrations.infrastructure.db.QontoConfig
import fr.devlille.partners.connect.integrations.infrastructure.db.QontoIntegrationsTable
import fr.devlille.partners.connect.integrations.infrastructure.db.get
import fr.devlille.partners.connect.internal.infrastructure.uuid.toUUID
import fr.devlille.partners.connect.partnership.domain.PartnershipPricing
import fr.devlille.partners.connect.partnership.infrastructure.db.BillingEntity
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

class QontoBillingGateway(
    private val qontoProvider: QontoProvider,
) : BillingGateway {
    override val provider: IntegrationProvider = IntegrationProvider.QONTO

    override suspend fun createInvoice(integrationId: UUID, pricing: PartnershipPricing): String {
        val config = transaction { QontoIntegrationsTable[integrationId] }
        val billing = transaction {
            BillingEntity.singleByEventAndPartnership(pricing.eventId.toUUID(), pricing.partnershipId.toUUID())
                ?: throw NotFoundException("No billing found for company ${pricing.partnershipId}")
        }
        val event = transaction { billing.event }
        val company = transaction { billing.partnership.company }
        val items = invoiceItems(pricing)
        val client = getClient(billing, company, config)
        val request = event.toQontoInvoiceRequest(
            clientId = client.id,
            invoicePo = billing.po,
            invoiceItems = items,
        )
        return qontoProvider.createInvoice(request, config).clientInvoice.invoiceUrl
    }

    override suspend fun createQuote(integrationId: UUID, pricing: PartnershipPricing): String {
        val config = transaction { QontoIntegrationsTable[integrationId] }
        val billing = transaction {
            BillingEntity.singleByEventAndPartnership(pricing.eventId.toUUID(), pricing.partnershipId.toUUID())
                ?: throw NotFoundException("No billing found for company ${pricing.partnershipId}")
        }
        val event = transaction { billing.event }
        val company = transaction { billing.partnership.company }
        val items = invoiceItems(pricing)
        val client = getClient(billing, company, config)
        val request = event.toQontoQuoteRequest(clientId = client.id, invoiceItems = items)
        return qontoProvider.createQuote(request, config).quote.quoteUrl
    }

    private suspend fun getClient(billing: BillingEntity, company: CompanyEntity, config: QontoConfig): QontoClient {
        val clients = qontoProvider.listClients(taxId = company.siret, config = config)
        return if (clients.clients.isEmpty()) {
            qontoProvider.createClient(billing.toQontoClientRequest(company), config).client
        } else {
            clients.clients.first()
        }
    }
}
