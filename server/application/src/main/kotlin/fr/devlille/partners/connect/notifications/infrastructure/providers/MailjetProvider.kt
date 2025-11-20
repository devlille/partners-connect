package fr.devlille.partners.connect.notifications.infrastructure.providers

import fr.devlille.partners.connect.integrations.infrastructure.db.MailjetConfig
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import io.ktor.http.headers
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class MailjetProvider(
    private val httpClient: HttpClient,
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
    suspend fun send(body: MailjetBody, config: MailjetConfig): Boolean {
        val basic = Base64.encode("${config.apiKey}:${config.secret}".toByteArray())
        val response = httpClient.post("https://api.mailjet.com/v3.1/send") {
            headers[HttpHeaders.Authorization] = "Basic $basic"
            headers[HttpHeaders.ContentType] = "application/json"
            setBody(Json.encodeToString(MailjetBody.serializer(), body))
        }
        return response.status.isSuccess()
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

@Serializable
data class StatCounters(
    @SerialName("Count")
    val count: Int,
    @SerialName("Total")
    val total: Int,
)
