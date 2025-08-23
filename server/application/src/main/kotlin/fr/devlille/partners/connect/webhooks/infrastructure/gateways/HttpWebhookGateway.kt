package fr.devlille.partners.connect.webhooks.infrastructure.gateways

import fr.devlille.partners.connect.integrations.infrastructure.db.WebhookIntegrationsTable
import fr.devlille.partners.connect.integrations.infrastructure.db.get
import fr.devlille.partners.connect.webhooks.domain.WebhookConfig
import fr.devlille.partners.connect.webhooks.domain.WebhookGateway
import fr.devlille.partners.connect.webhooks.domain.WebhookPayload
import io.ktor.client.HttpClient
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
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

class HttpWebhookGateway(
    private val httpClient: HttpClient,
) : WebhookGateway {
    override fun getIntegrationConfiguration(integrationId: UUID): WebhookConfig? {
        @Suppress("TooGenericExceptionCaught", "SwallowedException")
        return try {
            val config = WebhookIntegrationsTable[integrationId]
            WebhookConfig(
                url = config.url,
                headerAuth = config.headerAuth,
                type = config.type,
                partnershipId = config.partnershipId,
            )
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun canSendWebhook(eventId: UUID, integrationId: UUID): Boolean {
        // For now, always allow webhook sending if the configuration exists
        return getIntegrationConfiguration(integrationId) != null
    }

    override suspend fun sendHttpCall(integrationId: UUID, payload: WebhookPayload): Boolean = runBlocking {
        val config = getIntegrationConfiguration(integrationId) ?: return@runBlocking false

        @Suppress("TooGenericExceptionCaught", "SwallowedException")
        try {
            val response = httpClient.post(config.url) {
                contentType(ContentType.Application.Json)
                headers {
                    // Add authentication header if provided
                    config.headerAuth?.let { auth ->
                        append(HttpHeaders.Authorization, auth)
                    }
                    // Add webhook signature
                    generateSignature(payload)?.let { signature ->
                        append("X-Webhook-Signature", signature)
                    }
                }
                setBody(Json.encodeToString(WebhookPayload.serializer(), payload))
            }
            response.status.isSuccess()
        } catch (e: Exception) {
            false
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private fun generateSignature(payload: WebhookPayload): String? {
        return try {
            val secret = "webhook-secret" // Fixed secret for webhook signing
            val jsonPayload = Json.encodeToString(WebhookPayload.serializer(), payload)
            val mac = Mac.getInstance("HmacSHA256")
            val secretKey = SecretKeySpec(secret.toByteArray(), "HmacSHA256")
            mac.init(secretKey)
            val signature = mac.doFinal(jsonPayload.toByteArray())
            "sha256=" + Base64.encode(signature)
        } catch (e: Exception) {
            null
        }
    }
}
