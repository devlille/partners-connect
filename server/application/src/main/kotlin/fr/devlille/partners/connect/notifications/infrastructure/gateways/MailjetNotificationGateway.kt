package fr.devlille.partners.connect.notifications.infrastructure.gateways

import fr.devlille.partners.connect.integrations.domain.IntegrationProvider
import fr.devlille.partners.connect.integrations.infrastructure.db.MailjetIntegrationsTable
import fr.devlille.partners.connect.integrations.infrastructure.db.get
import fr.devlille.partners.connect.internal.infrastructure.resources.readResourceFile
import fr.devlille.partners.connect.internal.infrastructure.uuid.toUUID
import fr.devlille.partners.connect.notifications.domain.NotificationGateway
import fr.devlille.partners.connect.notifications.domain.NotificationVariables
import fr.devlille.partners.connect.notifications.infrastructure.providers.Contact
import fr.devlille.partners.connect.notifications.infrastructure.providers.MailjetBody
import fr.devlille.partners.connect.notifications.infrastructure.providers.MailjetProvider
import fr.devlille.partners.connect.notifications.infrastructure.providers.Message
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEmailEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEmailsTable
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipsTable
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.io.encoding.ExperimentalEncodingApi

class MailjetNotificationGateway(
    private val mailjetProvider: MailjetProvider,
) : NotificationGateway {
    override val provider = IntegrationProvider.MAILJET

    @Suppress("ReturnCount")
    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun send(integrationId: UUID, variables: NotificationVariables): Boolean {
        val config = transaction { MailjetIntegrationsTable[integrationId] }
        val emails = transaction {
            val partnership = PartnershipEntity
                .find { PartnershipsTable.companyId eq variables.company.id.toUUID() }
                .singleOrNull()
                ?: throw NotFoundException("No partnership found for company ${variables.company.id}")

            PartnershipEmailEntity
                .find { PartnershipEmailsTable.partnershipId eq partnership.id }
                .toList()
        }
        val pathHeader = "/notifications/email/${variables.usageName}/header.${variables.language}.txt"
        val pathContent = "/notifications/email/${variables.usageName}/content.${variables.language}.html"
        val subject = try {
            variables.populate(readResourceFile(pathHeader))
        } catch (_: IllegalArgumentException) {
            return false
        }
        val htmlPart = try {
            variables.populate(readResourceFile(pathContent))
        } catch (_: IllegalArgumentException) {
            return false
        }
        val body = MailjetBody(
            messages = listOf(
                Message(
                    from = Contact(email = variables.event.event.contact.email, name = variables.event.event.name),
                    to = emails.map { Contact(email = it.email) },
                    subject = "[${variables.event.event.name}] $subject",
                    htmlPart = htmlPart,
                ),
            ),
        )
        return mailjetProvider.send(body, config)
    }
}
