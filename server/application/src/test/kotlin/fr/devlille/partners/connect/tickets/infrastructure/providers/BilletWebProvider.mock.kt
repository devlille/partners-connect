package fr.devlille.partners.connect.tickets.infrastructure.providers

import fr.devlille.partners.connect.tickets.infrastructure.providers.models.CreateOrderResponseItem
import fr.devlille.partners.connect.tickets.infrastructure.providers.models.ProductDetail
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.serialization.json.Json
import java.util.UUID

fun HttpRequestData.isBilletWebProvider(): Boolean = url.host == "www.billetweb.fr"

fun MockRequestHandleScope.mockedBilletWebProviderResponse(
    nbProducts: Int,
): HttpResponseData {
    val response = CreateOrderResponseItem(
        id = UUID.randomUUID().toString(),
        productsDetails = (0.until(nbProducts)).map {
            ProductDetail(
                id = UUID.randomUUID().toString(),
                extId = UUID.randomUUID().toString(),
                productDownload = "https://example.com/download/$it",
            )
        },
    )
    return respond(
        content = Json.encodeToString(value = listOf(response)),
        status = HttpStatusCode.OK,
        headers = headersOf(HttpHeaders.ContentType, "application/json"),
    )
}
