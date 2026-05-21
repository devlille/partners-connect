package fr.devlille.partners.connect.ecosystempartners.application

import com.slack.api.Slack
import fr.devlille.partners.connect.ecosystempartners.domain.EcosystemPartnerLifecycleEvent
import fr.devlille.partners.connect.ecosystempartners.domain.EcosystemPartnerNotificationRepository
import fr.devlille.partners.connect.ecosystempartners.domain.publicEventUrl
import fr.devlille.partners.connect.ecosystempartners.infrastructure.db.EcosystemPartnerEmailEntity
import fr.devlille.partners.connect.ecosystempartners.infrastructure.db.EcosystemPartnerEntity
import fr.devlille.partners.connect.events.domain.EventRepository
import fr.devlille.partners.connect.integrations.domain.IntegrationProvider
import fr.devlille.partners.connect.integrations.domain.IntegrationUsage
import fr.devlille.partners.connect.integrations.infrastructure.db.IntegrationEntity
import fr.devlille.partners.connect.integrations.infrastructure.db.IntegrationsTable
import fr.devlille.partners.connect.integrations.infrastructure.db.MailjetIntegrationsTable
import fr.devlille.partners.connect.integrations.infrastructure.db.SlackIntegrationsTable
import fr.devlille.partners.connect.integrations.infrastructure.db.get
import fr.devlille.partners.connect.internal.infrastructure.resources.readResourceFile
import fr.devlille.partners.connect.notifications.infrastructure.providers.Contact
import fr.devlille.partners.connect.notifications.infrastructure.providers.MailjetBody
import fr.devlille.partners.connect.notifications.infrastructure.providers.MailjetProvider
import fr.devlille.partners.connect.notifications.infrastructure.providers.Message
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

/**
 * Dispatches ecosystem partner lifecycle notifications by talking to the raw
 * Mailjet and Slack providers directly. Bypasses the partnership-centric
 * `NotificationRepository` / `MailjetNotificationGateway` so we don't have to
 * fabricate a classic partnership row for every ecosystem partner email.
 */
class EcosystemPartnerNotificationRepositoryExposed(
    private val eventRepository: EventRepository,
    private val mailjetProvider: MailjetProvider,
    private val slack: Slack,
) : EcosystemPartnerNotificationRepository {
    override suspend fun notify(
        eventSlug: String,
        ecosystemPartnerId: UUID,
        lifecycle: EcosystemPartnerLifecycleEvent,
    ) {
        val context = transaction { loadContext(eventSlug, ecosystemPartnerId) }
        val eventWithOrganisation = eventRepository.getBySlug(eventSlug)
        val populate: (String) -> String = { content ->
            populateTemplate(content, context, eventWithOrganisation)
        }

        val integrations = transaction {
            IntegrationEntity
                .find {
                    IntegrationsTable.eventId eq context.eventId and
                        (IntegrationsTable.usage eq IntegrationUsage.NOTIFICATION)
                }
                .toList()
                .map { it.id.value to it.provider }
        }

        integrations.forEach { (integrationId, provider) ->
            when (provider) {
                IntegrationProvider.MAILJET -> sendMailjet(
                    integrationId = integrationId,
                    lifecycle = lifecycle,
                    context = context,
                    populate = populate,
                )
                IntegrationProvider.SLACK -> sendSlack(
                    integrationId = integrationId,
                    lifecycle = lifecycle,
                    context = context,
                    populate = populate,
                )
                else -> Unit
            }
        }
    }

    private suspend fun sendMailjet(
        integrationId: UUID,
        lifecycle: EcosystemPartnerLifecycleEvent,
        context: NotificationContext,
        populate: (String) -> String,
    ) {
        if (context.partnerEmails.isEmpty()) return
        val headerPath = "/notifications/email/${lifecycle.templateName}/header.${context.language}.txt"
        val contentPath = "/notifications/email/${lifecycle.templateName}/content.${context.language}.html"
        val subject = runCatching { populate(readResourceFile(headerPath)) }.getOrNull()
        val htmlBody = runCatching { populate(readResourceFile(contentPath)) }.getOrNull()
        if (subject == null || htmlBody == null) return
        val config = transaction { MailjetIntegrationsTable[integrationId] }
        val body = MailjetBody(
            messages = listOf(
                Message(
                    from = Contact(email = context.eventContactEmail, name = context.eventName),
                    to = context.partnerEmails.map { Contact(email = it, name = null) },
                    cc = null,
                    subject = "[${context.eventName}][${context.companyName}] $subject",
                    htmlPart = htmlBody,
                ),
            ),
        )
        mailjetProvider.send(body, config)
    }

    private fun sendSlack(
        integrationId: UUID,
        lifecycle: EcosystemPartnerLifecycleEvent,
        context: NotificationContext,
        populate: (String) -> String,
    ) {
        val path = "/notifications/slack/${lifecycle.templateName}/${context.language}.md"
        val message = runCatching { populate(readResourceFile(path)) }.getOrNull() ?: return
        val config = transaction { SlackIntegrationsTable[integrationId] }
        slack.methods(config.token).chatPostMessage {
            it.channel(config.channel).text(message)
        }
    }

    private fun loadContext(eventSlug: String, ecosystemPartnerId: UUID): NotificationContext {
        val entity = EcosystemPartnerEntity.findById(ecosystemPartnerId)
            ?: throw NotFoundException("Ecosystem partner $ecosystemPartnerId not found")
        if (entity.event.slug != eventSlug) {
            throw NotFoundException("Ecosystem partner $ecosystemPartnerId does not belong to event $eventSlug")
        }
        val emails = EcosystemPartnerEmailEntity
            .listByEcosystemPartner(ecosystemPartnerId)
            .map { it.email }
        return NotificationContext(
            eventId = entity.event.id.value,
            eventName = entity.event.name,
            eventContactEmail = entity.event.contactEmail,
            companyName = entity.company.name,
            categoryName = entity.category.name,
            language = entity.language,
            partnerEmails = emails,
        )
    }

    private fun populateTemplate(
        content: String,
        context: NotificationContext,
        eventWithOrganisation: fr.devlille.partners.connect.events.domain.EventWithOrganisation,
    ): String = content
        .replace("{{event_name}}", context.eventName)
        .replace("{{event_contact}}", context.eventContactEmail)
        .replace("{{company_name}}", context.companyName)
        .replace("{{category_name}}", context.categoryName)
        .replace("{{public_event_url}}", publicEventUrl(eventWithOrganisation))

    private data class NotificationContext(
        val eventId: UUID,
        val eventName: String,
        val eventContactEmail: String,
        val companyName: String,
        val categoryName: String,
        val language: String,
        val partnerEmails: List<String>,
    )
}
