package fr.devlille.partners.connect.notifications.infrastructure.gateways

import fr.devlille.partners.connect.events.domain.WebhookType
import fr.devlille.partners.connect.events.infrastructure.db.EventWebhookEntity
import fr.devlille.partners.connect.events.infrastructure.db.EventWebhooksTable
import fr.devlille.partners.connect.notifications.domain.NotificationVariables
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
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@Serializable
data class WebhookPayload(
    // "created", "updated", "deleted"
    val eventType: String,
    val partnership: PartnershipWebhookData,
    val event: EventWebhookData,
    val timestamp: String,
)

@Serializable
data class PartnershipWebhookData(
    val id: String,
    val companyId: String,
    val packId: String?,
    val status: String,
)

@Serializable
data class EventWebhookData(
    val id: String,
    val slug: String,
    val name: String,
)

class WebhookService(
    private val httpClient: HttpClient,
) {
    fun sendWebhooks(eventId: UUID, variables: NotificationVariables): Boolean = runBlocking {
        val partnershipId = when (variables) {
            is NotificationVariables.NewPartnership -> variables.partnership.id.let { UUID.fromString(it) }
            is NotificationVariables.PartnershipValidated -> variables.partnership.id.let { UUID.fromString(it) }
            is NotificationVariables.PartnershipDeclined -> variables.partnership.id.let { UUID.fromString(it) }
            else -> null
        }

        val webhooks = EventWebhookEntity.find {
            EventWebhooksTable.eventId eq eventId
        }.filter { webhook ->
            when (WebhookType.valueOf(webhook.type.uppercase())) {
                WebhookType.ALL -> true
                WebhookType.PARTNERSHIP -> partnershipId != null && webhook.partnership?.id?.value == partnershipId
            }
        }

        var allSuccessful = true
        for (webhook in webhooks) {
            val success = sendWebhookNotification(webhook, variables, eventId)
            if (!success) {
                allSuccessful = false
            }
        }
        allSuccessful
    }

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private suspend fun sendWebhookNotification(
        webhook: EventWebhookEntity,
        variables: NotificationVariables,
        eventId: UUID,
    ): Boolean {
        return try {
            val payload = createWebhookPayload(variables, eventId)
            val json = Json.encodeToString(WebhookPayload.serializer(), payload)
            val response = sendHttpRequest(webhook, json)
            response.status.isSuccess()
        } catch (e: Exception) {
            // In production, this should be properly logged with context
            false
        }
    }

    private fun createWebhookPayload(variables: NotificationVariables, eventId: UUID): WebhookPayload {
        val eventType = when (variables) {
            is NotificationVariables.NewPartnership -> "created"
            is NotificationVariables.PartnershipValidated -> "updated"
            is NotificationVariables.PartnershipDeclined -> "deleted"
            else -> "updated"
        }

        val partnershipData = when (variables) {
            is NotificationVariables.NewPartnership -> PartnershipWebhookData(
                id = variables.partnership.id,
                companyId = variables.company.id,
                packId = variables.pack.id,
                status = "pending",
            )
            is NotificationVariables.PartnershipValidated -> PartnershipWebhookData(
                id = variables.partnership.id,
                companyId = variables.company.id,
                packId = variables.pack.id,
                status = "validated",
            )
            is NotificationVariables.PartnershipDeclined -> PartnershipWebhookData(
                id = variables.partnership.id,
                companyId = variables.company.id,
                packId = null,
                status = "declined",
            )
            else -> PartnershipWebhookData(
                id = "",
                companyId = variables.company.id,
                packId = null,
                status = "unknown",
            )
        }

        return WebhookPayload(
            eventType = eventType,
            partnership = partnershipData,
            event = EventWebhookData(
                id = eventId.toString(),
                slug = variables.event.event.slug,
                name = variables.event.event.name,
            ),
            timestamp = kotlinx.datetime.Clock.System.now().toString(),
        )
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
