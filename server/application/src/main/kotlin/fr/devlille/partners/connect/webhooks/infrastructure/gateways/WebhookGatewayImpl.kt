package fr.devlille.partners.connect.webhooks.infrastructure.gateways

import fr.devlille.partners.connect.webhooks.domain.WebhookGateway
import fr.devlille.partners.connect.webhooks.domain.WebhookPayload
import fr.devlille.partners.connect.webhooks.domain.WebhookType
import fr.devlille.partners.connect.webhooks.infrastructure.db.EventWebhookEntity
import fr.devlille.partners.connect.webhooks.infrastructure.db.EventWebhooksTable
import io.ktor.client.HttpClient
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class WebhookGatewayImpl(
    private val httpClient: HttpClient,
) : WebhookGateway {
    override suspend fun sendWebhooks(eventId: UUID, payload: WebhookPayload): Boolean = runBlocking {
        val partnershipId = UUID.fromString(payload.partnership.id)

        val webhooks = EventWebhookEntity.find {
            EventWebhooksTable.eventId eq eventId
        }.filter { webhook ->
            when (WebhookType.valueOf(webhook.type.uppercase())) {
                WebhookType.ALL -> true
                WebhookType.PARTNERSHIP -> webhook.partnership?.id?.value == partnershipId
            }
        }

        var allSuccessful = true
        for (webhook in webhooks) {
            val success = sendWebhookNotification(webhook, payload)
            if (!success) {
                allSuccessful = false
            }
        }
        allSuccessful
    }

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private suspend fun sendWebhookNotification(
        webhook: EventWebhookEntity,
        payload: WebhookPayload,
    ): Boolean {
        return try {
            val json = Json.encodeToString(WebhookPayload.serializer(), payload)
            val response = sendHttpRequest(webhook, json)
            response.status.isSuccess()
        } catch (e: Exception) {
            // In production, this should be properly logged with context
            false
        }
    }

    private suspend fun sendHttpRequest(webhook: EventWebhookEntity, json: String): HttpResponse {
        return httpClient.post(webhook.url) {
            contentType(ContentType.Application.Json)
            setBody(json)

            // Add authentication header if configured
            webhook.headerAuth?.let { authHeader ->
                headers {
                    append(HttpHeaders.Authorization, authHeader)
                }
            }

            // Add signature header for webhook verification
            webhook.headerAuth?.let { secret ->
                val signature = generateHmacSignature(json, secret)
                headers {
                    append("X-Webhook-Signature", "sha256=$signature")
                }
            }
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun generateHmacSignature(payload: String, secret: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        val secretKey = SecretKeySpec(secret.toByteArray(), "HmacSHA256")
        mac.init(secretKey)
        val signature = mac.doFinal(payload.toByteArray())
        return Base64.encode(signature)
    }
}
