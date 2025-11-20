package fr.devlille.partners.connect.tickets.infrastructure.providers

import fr.devlille.partners.connect.integrations.infrastructure.db.BilletWebConfig
import fr.devlille.partners.connect.tickets.infrastructure.providers.models.CreateOrderProduct
import fr.devlille.partners.connect.tickets.infrastructure.providers.models.CreateOrderRequest
import fr.devlille.partners.connect.tickets.infrastructure.providers.models.CreateOrderResponseItem
import fr.devlille.partners.connect.tickets.infrastructure.providers.models.IdentifierResponse
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

class BilletWebProvider(
    private val httpClient: HttpClient,
) {
    suspend fun listTariffs(config: BilletWebConfig): List<IdentifierResponse> {
        val route = "https://www.billetweb.fr/api/event/${config.eventId}/tickets?version=1"
        val response = httpClient.get(route) {
            headers[HttpHeaders.Authorization] = "Basic ${config.basic}"
            headers[HttpHeaders.ContentType] = "application/json"
        }
        return response.body<List<IdentifierResponse>>()
    }

    suspend fun createOrder(request: CreateOrderRequest, config: BilletWebConfig): CreateOrderResponseItem {
        val route = "https://www.billetweb.fr/api/event/${config.eventId}/add_order"
        val response = httpClient.post(route) {
            headers[HttpHeaders.Authorization] = "Basic ${config.basic}"
            headers[HttpHeaders.ContentType] = "application/json"
            setBody(Json.encodeToString(ListSerializer(CreateOrderRequest.serializer()), listOf(request)))
        }
        return response.body<List<CreateOrderResponseItem>>().first()
    }

    suspend fun updateProduct(request: CreateOrderProduct, config: BilletWebConfig) {
        val route = "https://www.billetweb.fr/api/event/${config.eventId}/update_product"
        val response = httpClient.post(route) {
            headers[HttpHeaders.Authorization] = "Basic ${config.basic}"
            headers[HttpHeaders.ContentType] = "application/json"
            setBody(Json.encodeToString(ListSerializer(CreateOrderProduct.serializer()), listOf(request)))
        }
    }
}
