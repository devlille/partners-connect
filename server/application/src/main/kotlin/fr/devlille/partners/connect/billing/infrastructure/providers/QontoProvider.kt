package fr.devlille.partners.connect.billing.infrastructure.providers

import fr.devlille.partners.connect.billing.infrastructure.gateways.models.QontoClient
import fr.devlille.partners.connect.billing.infrastructure.gateways.models.QontoClientInvoiceResponse
import fr.devlille.partners.connect.billing.infrastructure.gateways.models.QontoClientRequest
import fr.devlille.partners.connect.billing.infrastructure.gateways.models.QontoClientResponse
import fr.devlille.partners.connect.billing.infrastructure.gateways.models.QontoClientsResponse
import fr.devlille.partners.connect.billing.infrastructure.gateways.models.QontoInvoiceRequest
import fr.devlille.partners.connect.billing.infrastructure.gateways.models.QontoQuoteRequest
import fr.devlille.partners.connect.billing.infrastructure.gateways.models.QontoQuoteResponse
import fr.devlille.partners.connect.billing.infrastructure.gateways.models.mappers.toQontoClientRequest
import fr.devlille.partners.connect.integrations.infrastructure.db.QontoConfig
import fr.devlille.partners.connect.internal.infrastructure.system.SystemVarEnv
import fr.devlille.partners.connect.partnership.infrastructure.db.BillingEntity
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import kotlinx.serialization.json.Json

class QontoProvider(
    private val httpClient: HttpClient,
) {
    suspend fun getClient(billing: BillingEntity, config: QontoConfig): QontoClient {
        val clients = listClients(taxId = billing.partnership.company.siret, config = config)
        return if (clients.clients.isEmpty()) {
            createClient(billing.toQontoClientRequest(), config).client
        } else {
            clients.clients.first()
        }
    }

    suspend fun listClients(taxId: String?, config: QontoConfig): QontoClientsResponse {
        val taxFilter = if (taxId.isNullOrBlank()) {
            ""
        } else {
            "?filter[tax_identification_number]=$taxId"
        }
        val route = "${SystemVarEnv.QontoProvider.baseUrl}/v2/clients$taxFilter"
        val response = httpClient.get(route) {
            headers[HttpHeaders.Authorization] = "${config.apiKey}:${config.secret}"
            config.sandboxToken?.let {
                headers["X-Qonto-Staging-Token"] = it
            }
            headers[HttpHeaders.ContentType] = "application/json"
        }
        return response.body<QontoClientsResponse>()
    }

    suspend fun createClient(request: QontoClientRequest, config: QontoConfig): QontoClientResponse {
        val route = "${SystemVarEnv.QontoProvider.baseUrl}/v2/clients"
        val response = httpClient.post(route) {
            headers[HttpHeaders.Authorization] = "${config.apiKey}:${config.secret}"
            config.sandboxToken?.let {
                headers["X-Qonto-Staging-Token"] = it
            }
            headers[HttpHeaders.ContentType] = "application/json"
            setBody(Json.encodeToString(QontoClientRequest.serializer(), request))
        }
        return response.body<QontoClientResponse>()
    }

    suspend fun createInvoice(request: QontoInvoiceRequest, config: QontoConfig): QontoClientInvoiceResponse {
        val route = "${SystemVarEnv.QontoProvider.baseUrl}/v2/client_invoices"
        val response = httpClient.post(route) {
            headers[HttpHeaders.Authorization] = "${config.apiKey}:${config.secret}"
            config.sandboxToken?.let {
                headers["X-Qonto-Staging-Token"] = it
            }
            headers[HttpHeaders.ContentType] = "application/json"
            setBody(Json.encodeToString(QontoInvoiceRequest.serializer(), request))
        }
        return response.body<QontoClientInvoiceResponse>()
    }

    suspend fun createQuote(request: QontoQuoteRequest, config: QontoConfig): QontoQuoteResponse {
        val route = "${SystemVarEnv.QontoProvider.baseUrl}/v2/quotes"
        val response = httpClient.post(route) {
            headers[HttpHeaders.Authorization] = "${config.apiKey}:${config.secret}"
            config.sandboxToken?.let {
                headers["X-Qonto-Staging-Token"] = it
            }
            headers[HttpHeaders.ContentType] = "application/json"
            setBody(Json.encodeToString(QontoQuoteRequest.serializer(), request))
        }
        return response.body<QontoQuoteResponse>()
    }
}
