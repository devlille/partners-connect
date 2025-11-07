package fr.devlille.partners.connect.billing.infrastructure.gateways

import fr.devlille.partners.connect.billing.domain.BillingGateway
import fr.devlille.partners.connect.billing.infrastructure.gateways.models.QontoClient
import fr.devlille.partners.connect.billing.infrastructure.gateways.models.QontoClientInvoiceResponse
import fr.devlille.partners.connect.billing.infrastructure.gateways.models.QontoClientRequest
import fr.devlille.partners.connect.billing.infrastructure.gateways.models.QontoClientResponse
import fr.devlille.partners.connect.billing.infrastructure.gateways.models.QontoClientsResponse
import fr.devlille.partners.connect.billing.infrastructure.gateways.models.QontoInvoiceRequest
import fr.devlille.partners.connect.billing.infrastructure.gateways.models.QontoQuoteRequest
import fr.devlille.partners.connect.billing.infrastructure.gateways.models.QontoQuoteResponse
import fr.devlille.partners.connect.billing.infrastructure.gateways.models.mappers.invoiceItems
import fr.devlille.partners.connect.billing.infrastructure.gateways.models.mappers.toQontoClientRequest
import fr.devlille.partners.connect.billing.infrastructure.gateways.models.mappers.toQontoInvoiceRequest
import fr.devlille.partners.connect.billing.infrastructure.gateways.models.mappers.toQontoQuoteRequest
import fr.devlille.partners.connect.integrations.domain.IntegrationProvider
import fr.devlille.partners.connect.integrations.infrastructure.db.QontoConfig
import fr.devlille.partners.connect.integrations.infrastructure.db.QontoIntegrationsTable
import fr.devlille.partners.connect.integrations.infrastructure.db.get
import fr.devlille.partners.connect.internal.infrastructure.system.SystemVarEnv
import fr.devlille.partners.connect.internal.infrastructure.uuid.toUUID
import fr.devlille.partners.connect.partnership.domain.PartnershipPricing
import fr.devlille.partners.connect.partnership.infrastructure.db.BillingEntity
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import io.ktor.server.plugins.NotFoundException
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

class QontoBillingGateway(
    private val httpClient: HttpClient,
) : BillingGateway {
    override val provider: IntegrationProvider = IntegrationProvider.QONTO

    override suspend fun createInvoice(integrationId: UUID, pricing: PartnershipPricing): String {
        val config = transaction { QontoIntegrationsTable[integrationId] }
        val billing = transaction {
            BillingEntity.singleByEventAndPartnership(pricing.eventId.toUUID(), pricing.partnershipId.toUUID())
                ?: throw NotFoundException("No billing found for company ${pricing.partnershipId}")
        }
        val items = invoiceItems(pricing)
        val client = getClient(billing, config)
        val request = billing.event.toQontoInvoiceRequest(
            clientId = client.id,
            invoicePo = billing.po,
            invoiceItems = items,
        )
        return createInvoice(request, config).clientInvoice.invoiceUrl
    }

    override suspend fun createQuote(integrationId: UUID, pricing: PartnershipPricing): String {
        val config = transaction { QontoIntegrationsTable[integrationId] }
        val billing = transaction {
            BillingEntity.singleByEventAndPartnership(pricing.eventId.toUUID(), pricing.partnershipId.toUUID())
                ?: throw NotFoundException("No billing found for company ${pricing.partnershipId}")
        }
        val items = invoiceItems(pricing)
        val client = getClient(billing, config)
        val request = billing.event.toQontoQuoteRequest(clientId = client.id, invoiceItems = items)
        return createQuote(request, config).quoteUrl
    }

    private suspend fun getClient(billing: BillingEntity, config: QontoConfig): QontoClient {
        val clients = listClients(taxId = billing.partnership.company.siret, config = config)
        return if (clients.clients.isEmpty()) {
            createClient(billing.toQontoClientRequest(), config).client
        } else {
            clients.clients.first()
        }
    }

    private suspend fun listClients(taxId: String, config: QontoConfig): QontoClientsResponse {
        val route = "${SystemVarEnv.QontoProvider.baseUrl}/v2/clients?filter[tax_identification_number]=$taxId"
        val response = httpClient.get(route) {
            headers[HttpHeaders.Authorization] = "${config.apiKey}:${config.secret}"
            headers["X-Qonto-Staging-Token"] = config.sandboxToken
            headers[HttpHeaders.ContentType] = "application/json"
        }
        return response.body<QontoClientsResponse>()
    }

    private suspend fun createClient(request: QontoClientRequest, config: QontoConfig): QontoClientResponse {
        val route = "${SystemVarEnv.QontoProvider.baseUrl}/v2/clients"
        val response = httpClient.post(route) {
            headers[HttpHeaders.Authorization] = "${config.apiKey}:${config.secret}"
            headers["X-Qonto-Staging-Token"] = config.sandboxToken
            headers[HttpHeaders.ContentType] = "application/json"
            setBody(Json.encodeToString(QontoClientRequest.serializer(), request))
        }
        return response.body<QontoClientResponse>()
    }

    private suspend fun createInvoice(request: QontoInvoiceRequest, config: QontoConfig): QontoClientInvoiceResponse {
        val route = "${SystemVarEnv.QontoProvider.baseUrl}/v2/client_invoices"
        val response = httpClient.post(route) {
            headers[HttpHeaders.Authorization] = "${config.apiKey}:${config.secret}"
            headers["X-Qonto-Staging-Token"] = config.sandboxToken
            headers[HttpHeaders.ContentType] = "application/json"
            setBody(Json.encodeToString(QontoInvoiceRequest.serializer(), request))
        }
        return response.body<QontoClientInvoiceResponse>()
    }

    private suspend fun createQuote(request: QontoQuoteRequest, config: QontoConfig): QontoQuoteResponse {
        val route = "${SystemVarEnv.QontoProvider.baseUrl}/v2/quotes"
        val response = httpClient.post(route) {
            headers[HttpHeaders.Authorization] = "${config.apiKey}:${config.secret}"
            headers["X-Qonto-Staging-Token"] = config.sandboxToken
            headers[HttpHeaders.ContentType] = "application/json"
            setBody(Json.encodeToString(QontoQuoteRequest.serializer(), request))
        }
        return response.body<QontoQuoteResponse>()
    }
}
