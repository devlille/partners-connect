package fr.devlille.partners.connect.notifications.infrastructure.gateways

import fr.devlille.partners.connect.integrations.domain.IntegrationProvider
import fr.devlille.partners.connect.integrations.infrastructure.db.MailjetIntegrationsTable
import fr.devlille.partners.connect.integrations.infrastructure.db.get
import fr.devlille.partners.connect.internal.infrastructure.resources.readResourceFile
import fr.devlille.partners.connect.notifications.domain.NotificationGateway
import fr.devlille.partners.connect.notifications.domain.NotificationVariables
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEmailEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEmailsTable
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipsTable
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.server.plugins.NotFoundException
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.UUID
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class MailjetNotificationGateway(
    private val httpClient: HttpClient,
) : NotificationGateway {
    override val provider = IntegrationProvider.MAILJET

    @OptIn(ExperimentalEncodingApi::class)
    override fun send(integrationId: UUID, variables: NotificationVariables): Boolean = runBlocking {
        val pathHeader = "/notifications/email/${variables.usageName}/header.${variables.language}.txt"
        val pathContent = "/notifications/email/${variables.usageName}/content.${variables.language}.html"
        val config = MailjetIntegrationsTable[integrationId]
        val partnership = PartnershipEntity
            .find { PartnershipsTable.companyId eq UUID.fromString(variables.company.id) }
            .singleOrNull()
            ?: throw NotFoundException("No partnership found for company ${variables.company.id}")
        val emails = PartnershipEmailEntity
            .find { PartnershipEmailsTable.partnershipId eq partnership.id }
            .toList()
        val body = MailjetBody(
            messages = listOf(
                Message(
                    from = Contact(email = variables.event.contactEmail, name = variables.event.name),
                    to = emails.map { Contact(email = it.email) },
                    subject = "[${variables.event.name}] ${variables.populate(readResourceFile(pathHeader))}",
                    htmlPart = variables.populate(readResourceFile(pathContent)),
                ),
            ),
        )
        val basic = Base64.encode("${config.apiKey}:${config.secret}".toByteArray())
        val response = httpClient.post("https://api.mailjet.com/v3.1/send") {
            headers[HttpHeaders.Authorization] = "Basic $basic"
            headers[HttpHeaders.ContentType] = "application/json"
            setBody(Json.encodeToString(MailjetBody.serializer(), body))
        }
        response.status.isSuccess()
    }
}

@Serializable
private class MailjetBody(
    @SerialName("Messages")
    val messages: List<Message>,
)

@Serializable
private class Message(
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
private class Contact(
    @SerialName("Email")
    val email: String,
    @SerialName("Name")
    val name: String? = null,
)
