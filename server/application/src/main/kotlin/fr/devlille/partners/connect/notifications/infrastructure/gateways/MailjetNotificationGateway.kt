package fr.devlille.partners.connect.notifications.infrastructure.gateways

import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.integrations.domain.IntegrationProvider
import fr.devlille.partners.connect.integrations.infrastructure.db.MailjetIntegrationsTable
import fr.devlille.partners.connect.integrations.infrastructure.db.get
import fr.devlille.partners.connect.internal.infrastructure.api.ForbiddenException
import fr.devlille.partners.connect.internal.infrastructure.resources.readResourceFile
import fr.devlille.partners.connect.internal.infrastructure.system.SystemVarEnv
import fr.devlille.partners.connect.internal.infrastructure.uuid.toUUID
import fr.devlille.partners.connect.notifications.domain.Destination
import fr.devlille.partners.connect.notifications.domain.NotificationGateway
import fr.devlille.partners.connect.notifications.domain.NotificationVariables
import fr.devlille.partners.connect.notifications.infrastructure.providers.Contact
import fr.devlille.partners.connect.notifications.infrastructure.providers.MailjetBody
import fr.devlille.partners.connect.notifications.infrastructure.providers.MailjetProvider
import fr.devlille.partners.connect.notifications.infrastructure.providers.Message
import fr.devlille.partners.connect.partnership.application.mappers.toDomain
import fr.devlille.partners.connect.partnership.domain.PartnershipItem
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEmailEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipsTable
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.io.encoding.ExperimentalEncodingApi

internal class MailjetDestination(
    val from: EmailContact,
    val to: List<EmailContact>,
    val cc: List<EmailContact> = emptyList(),
    val footer: String? = null,
) : Destination

internal class EmailContact(
    val email: String,
    val name: String? = null,
)

class MailjetNotificationGateway(
    private val mailjetProvider: MailjetProvider,
) : NotificationGateway {
    override val provider = IntegrationProvider.MAILJET

    override fun getDestination(eventId: UUID, partnership: PartnershipItem): Destination = transaction {
        val event = EventEntity.findById(eventId) ?: throw NotFoundException("Event with ID $eventId not found")
        val orgContact = partnership.organiser?.let { EmailContact(email = it.email, name = it.displayName) }
        val eventContact = EmailContact(email = event.contactEmail, name = event.name)
        val footerPath = "/mailing/footer/content.${partnership.language}.html"
        val footer = try {
            readResourceFile(footerPath)
                .replace("{{event_name}}", event.name)
                .replace("{{event_contact}}", event.contactEmail)
                .replace("{{partnership_link}}", "${SystemVarEnv.frontendBaseUrl}/${event.slug}/$id")
        } catch (_: IllegalArgumentException) {
            null
        }
        MailjetDestination(
            from = orgContact ?: eventContact,
            to = partnership.emails.map { EmailContact(email = it, name = null) },
            cc = orgContact?.let { listOf(it, eventContact) } ?: listOf(eventContact),
            footer = footer,
        )
    }

    @Suppress("ReturnCount")
    @OptIn(ExperimentalEncodingApi::class)
    override suspend fun send(integrationId: UUID, variables: NotificationVariables): Boolean {
        val config = transaction { MailjetIntegrationsTable[integrationId] }
        val partnershipItem = transaction {
            val partnership = PartnershipEntity
                .find { PartnershipsTable.companyId eq variables.company.id.toUUID() }
                .singleOrNull()
                ?: throw NotFoundException("No partnership found for company ${variables.company.id}")
            partnership.toDomain(PartnershipEmailEntity.emails(partnership.id.value).toList())
        }
        val destination = getDestination(variables.event.event.id.toUUID(), partnershipItem)
        val mailDestination = destination as? MailjetDestination
            ?: throw ForbiddenException("Invalid destination type for Mailjet")
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
                    from = Contact(email = mailDestination.from.email, name = mailDestination.from.name),
                    to = mailDestination.to.map { Contact(email = it.email, name = it.name) },
                    cc = mailDestination.cc.map { Contact(email = it.email, name = it.name) },
                    subject = "[${variables.event.event.name}] $subject",
                    htmlPart = htmlPart,
                ),
            ),
        )
        return mailjetProvider.send(body, config)
    }

    override suspend fun send(
        integrationId: UUID,
        header: String,
        body: String,
        destination: Destination,
    ): Boolean {
        val config = transaction { MailjetIntegrationsTable[integrationId] }
        val mailDestination = destination as? MailjetDestination
            ?: throw ForbiddenException("Invalid destination type for Mailjet")
        val mailjetBody = MailjetBody(
            messages = listOf(
                Message(
                    from = Contact(email = mailDestination.from.email, name = mailDestination.from.name),
                    to = mailDestination.to.map { Contact(email = it.email, name = it.name) },
                    cc = mailDestination.cc.map { Contact(email = it.email, name = it.name) },
                    subject = header,
                    htmlPart = "$body<br><br>${mailDestination.footer ?: ""}",
                ),
            ),
        )
        return mailjetProvider.send(mailjetBody, config)
    }
}
