package fr.devlille.partners.connect.billing.infrastructure.gateways

import fr.devlille.partners.connect.billing.domain.BillingGateway
import fr.devlille.partners.connect.billing.infrastructure.gateways.models.QontoClient
import fr.devlille.partners.connect.billing.infrastructure.gateways.models.QontoClientInvoiceResponse
import fr.devlille.partners.connect.billing.infrastructure.gateways.models.QontoClientRequest
import fr.devlille.partners.connect.billing.infrastructure.gateways.models.QontoClientResponse
import fr.devlille.partners.connect.billing.infrastructure.gateways.models.QontoClientsResponse
import fr.devlille.partners.connect.billing.infrastructure.gateways.models.QontoInvoiceItem
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
import fr.devlille.partners.connect.partnership.infrastructure.db.BillingEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.singleByEventAndPartnership
import fr.devlille.partners.connect.partnership.infrastructure.db.validatedPack
import fr.devlille.partners.connect.sponsoring.infrastructure.db.PackOptionsTable
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringOptionEntity
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringOptionsTable
import fr.devlille.partners.connect.sponsoring.infrastructure.db.listOptionalOptionsByPack
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import io.ktor.server.plugins.NotFoundException
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.transactions.experimental.suspendedTransactionAsync
import java.util.UUID

class QontoBillingGateway(
    private val httpClient: HttpClient,
) : BillingGateway {
    override val provider: IntegrationProvider = IntegrationProvider.QONTO

    override suspend fun createInvoice(integrationId: UUID, eventId: UUID, partnershipId: UUID): String =
        suspendedTransactionAsync {
            val config = QontoIntegrationsTable[integrationId]
            val billing = BillingEntity.singleByEventAndPartnership(eventId, partnershipId)
                ?: throw NotFoundException("No billing found for company $partnershipId")
            val items = invoiceItems(eventId, billing)
            val client = getClient(billing, config)
            val request = billing.event.toQontoInvoiceRequest(
                clientId = client.id,
                invoicePo = billing.po,
                invoiceItems = items,
            )
            createInvoice(request, config).clientInvoice.invoiceUrl
        }.await()

    override suspend fun createQuote(integrationId: UUID, eventId: UUID, partnershipId: UUID): String =
        suspendedTransactionAsync {
            val config = QontoIntegrationsTable[integrationId]
            val billing = BillingEntity.singleByEventAndPartnership(eventId, partnershipId)
                ?: throw NotFoundException("No billing found for company $partnershipId")
            val items = invoiceItems(eventId, billing)
            val client = getClient(billing, config)
            val request = billing.event.toQontoQuoteRequest(clientId = client.id, invoiceItems = items)
            createQuote(request, config).quoteUrl
        }.await()

    private suspend fun getClient(billing: BillingEntity, config: QontoConfig): QontoClient {
        val clients = listClients(taxId = billing.partnership.company.siret, config = config)
        return if (clients.clients.isEmpty()) {
            createClient(billing.toQontoClientRequest(), config).client
        } else {
            clients.clients.first()
        }
    }

    private fun invoiceItems(eventId: UUID, billing: BillingEntity): List<QontoInvoiceItem> {
        val pack = billing.partnership.validatedPack()
            ?: throw NotFoundException("No sponsoring pack found for partnership ${billing.partnership.id}")
        val optionIds = PackOptionsTable.listOptionalOptionsByPack(pack.id.value)
            .map { it[PackOptionsTable.option].value }
        val optionalOptions = SponsoringOptionEntity
            .find { (SponsoringOptionsTable.eventId eq eventId) and (SponsoringOptionsTable.id inList optionIds) }
            .toList()
        return invoiceItems(billing.partnership.language, pack, optionalOptions)
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
