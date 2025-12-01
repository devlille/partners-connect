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
import fr.devlille.partners.connect.internal.infrastructure.api.ForbiddenException
import fr.devlille.partners.connect.internal.infrastructure.uuid.toUUID
import fr.devlille.partners.connect.partnership.domain.PartnershipDetail
import fr.devlille.partners.connect.partnership.infrastructure.db.BillingEntity
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

class QontoBillingGateway(
    private val qontoProvider: QontoProvider,
) : BillingGateway {
    override val provider: IntegrationProvider = IntegrationProvider.QONTO

    override suspend fun createInvoice(integrationId: UUID, eventId: UUID, partnership: PartnershipDetail): String {
        val config = transaction { QontoIntegrationsTable[integrationId] }
        val billing = transaction {
            BillingEntity.singleByEventAndPartnership(eventId, partnership.id.toUUID())
                ?: throw NotFoundException("No billing found for company ${partnership.id}")
        }
        val event = transaction { billing.event }
        val company = transaction { billing.partnership.company }
        val items = invoiceItems(partnership)
        val client = getClient(billing, company, config)
        val request = event.toQontoInvoiceRequest(
            clientId = client.id,
            invoicePo = billing.po,
            invoiceItems = items,
        )
        return qontoProvider.createInvoice(request, config).clientInvoice.invoiceUrl
    }

    override suspend fun createQuote(integrationId: UUID, eventId: UUID, partnership: PartnershipDetail): String {
        val config = transaction { QontoIntegrationsTable[integrationId] }
        val billing = transaction {
            BillingEntity.singleByEventAndPartnership(eventId, partnership.id.toUUID())
                ?: throw NotFoundException("No billing found for company ${partnership.id}")
        }
        val event = transaction { billing.event }
        val company = transaction { billing.partnership.company }
        val items = invoiceItems(partnership)
        val client = getClient(billing, company, config)
        val request = event.toQontoQuoteRequest(clientId = client.id, invoiceItems = items)
        return qontoProvider.createQuote(request, config).quote.quoteUrl
    }

    private suspend fun getClient(billing: BillingEntity, company: CompanyEntity, config: QontoConfig): QontoClient {
        if (company.siret == null) {
            throw ForbiddenException("Siret is required to create or find a Qonto client")
        }
        val clients = qontoProvider.listClients(taxId = company.siret, config = config)
        return if (clients.clients.isEmpty()) {
            qontoProvider.createClient(billing.toQontoClientRequest(company), config).client
        } else {
            val client = clients.clients.first()
            if (client.billingAddress == null) {
                qontoProvider.updateClient(billing.toQontoClientRequest(company), client.id, config).client
            } else {
                client
            }
        }
    }
}
