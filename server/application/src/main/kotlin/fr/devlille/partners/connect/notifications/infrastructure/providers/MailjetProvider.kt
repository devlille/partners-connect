package fr.devlille.partners.connect.notifications.infrastructure.providers

import fr.devlille.partners.connect.integrations.infrastructure.db.MailjetConfig
import fr.devlille.partners.connect.notifications.domain.RecipientResult
import fr.devlille.partners.connect.notifications.infrastructure.gateways.EmailDeliveryResult
import fr.devlille.partners.connect.partnership.domain.DeliveryStatus
import fr.devlille.partners.connect.partnership.domain.OverallDeliveryStatus
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class MailjetProvider(
    private val httpClient: HttpClient,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    @OptIn(ExperimentalEncodingApi::class)
    suspend fun status(config: MailjetConfig): Boolean {
        val basic = Base64.encode("${config.apiKey}:${config.secret}".toByteArray())
        val response = httpClient.get("https://api.mailjet.com/v3/REST/myprofile") {
            headers[HttpHeaders.Authorization] = "Basic $basic"
            headers[HttpHeaders.ContentType] = "application/json"
        }
        return response.status.isSuccess()
    }

    @OptIn(ExperimentalEncodingApi::class)
    suspend fun send(body: MailjetBody, config: MailjetConfig): EmailDeliveryResult {
        val basic = Base64.encode("${config.apiKey}:${config.secret}".toByteArray())
        val response = httpClient.post("https://api.mailjet.com/v3.1/send") {
            headers[HttpHeaders.Authorization] = "Basic $basic"
            headers[HttpHeaders.ContentType] = "application/json"
            setBody(Json.encodeToString(MailjetBody.serializer(), body))
        }

        // Extract email metadata from first message
        val firstMessage = body.messages.firstOrNull()
        val subject = firstMessage?.subject ?: ""
        val htmlBody = firstMessage?.htmlPart ?: ""
        val senderEmail = firstMessage?.from?.email ?: ""

        // If HTTP request failed, all recipients failed
        if (!response.status.isSuccess()) {
            val allRecipients = body.messages.flatMap { message ->
                (message.to + (message.cc ?: emptyList())).map { contact ->
                    RecipientResult(value = contact.email, status = DeliveryStatus.FAILED)
                }
            }
            return EmailDeliveryResult(
                overallStatus = OverallDeliveryStatus.FAILED,
                recipients = allRecipients,
                subject = subject,
                body = htmlBody,
                senderEmail = senderEmail,
            )
        }

        // Parse Mailjet response to extract per-recipient status
        val mailjetResponse = json.decodeFromString<MailjetResponse>(response.bodyAsText())

        // Extract successful recipients from Mailjet "Sent" array
        val successfulEmails = mailjetResponse.messages
            .filter { it.status == "success" }
            .flatMap { it.to + (it.cc ?: emptyList()) }
            .map { it.email }

        // Extract all recipient emails from request
        val allRecipientEmails = body.messages
            .flatMap { message -> (message.to + (message.cc ?: emptyList())).map { it.email } }
            .distinct()

        // Build per-recipient results
        val recipientResults = allRecipientEmails.map { email ->
            RecipientResult(
                value = email,
                status = if (email in successfulEmails) DeliveryStatus.SENT else DeliveryStatus.FAILED,
            )
        }

        // Compute overall status
        val overallStatus = when {
            recipientResults.all { it.status == DeliveryStatus.SENT } -> OverallDeliveryStatus.SENT
            recipientResults.all { it.status == DeliveryStatus.FAILED } -> OverallDeliveryStatus.FAILED
            else -> OverallDeliveryStatus.PARTIAL
        }

        return EmailDeliveryResult(
            overallStatus = overallStatus,
            recipients = recipientResults,
            subject = subject,
            body = htmlBody,
            senderEmail = senderEmail,
        )
    }
}

@Serializable
data class MailjetBody(
    @SerialName("Messages")
    val messages: List<Message>,
)

@Serializable
data class Message(
    @SerialName("From")
    val from: Contact,
    @SerialName("To")
    val to: List<Contact>,
    @SerialName("Cc")
    val cc: List<Contact>? = null,
    @SerialName("Subject")
    val subject: String,
    @SerialName("HTMLPart")
    val htmlPart: String,
)

@Serializable
data class Contact(
    @SerialName("Email")
    val email: String,
    @SerialName("Name")
    val name: String? = null,
)

/**
 * Mailjet v3.1 /send response structure
 */
@Serializable
data class MailjetResponse(
    @SerialName("Messages")
    val messages: List<MessageResponse>,
)

@Serializable
data class MessageResponse(
    @SerialName("Status")
    val status: String,
    @SerialName("To")
    val to: List<SentRecipient>,
    @SerialName("Cc")
    val cc: List<SentRecipient>? = null,
)

@Serializable
data class SentRecipient(
    @SerialName("Email")
    val email: String,
    @SerialName("MessageID")
    val messageId: Long? = null,
)
