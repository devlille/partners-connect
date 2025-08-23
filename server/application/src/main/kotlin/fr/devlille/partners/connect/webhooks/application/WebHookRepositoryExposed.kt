package fr.devlille.partners.connect.webhooks.application

import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.events.infrastructure.db.findBySlug
import fr.devlille.partners.connect.internal.infrastructure.uuid.toUUID
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEntity
import fr.devlille.partners.connect.webhooks.domain.CreateEventWebhookRequest
import fr.devlille.partners.connect.webhooks.domain.EventWebhook
import fr.devlille.partners.connect.webhooks.domain.WebhookRepository
import fr.devlille.partners.connect.webhooks.domain.WebhookType
import fr.devlille.partners.connect.webhooks.infrastructure.db.EventWebhookEntity
import fr.devlille.partners.connect.webhooks.infrastructure.db.EventWebhooksTable
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

class WebHookRepositoryExposed : WebhookRepository {
    override fun createWebhook(eventSlug: String, request: CreateEventWebhookRequest): UUID = transaction {
        // Validate URL is not empty
        if (request.url.isBlank()) {
            throw BadRequestException("Webhook URL cannot be empty")
        }

        // Basic URL validation
        val urlPattern = Regex("^https?://.*")
        if (!urlPattern.matches(request.url)) {
            throw BadRequestException("Invalid URL format - must start with http:// or https://")
        }

        // Validate partnership ID if type is PARTNERSHIP
        if (request.type == WebhookType.PARTNERSHIP) {
            if (request.partnershipId.isNullOrBlank()) {
                throw BadRequestException("Partnership ID is required when type is 'partnership'")
            }
        }

        val eventEntity = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")

        val webhookEntity = EventWebhookEntity.new {
            this.event = eventEntity
            this.url = request.url
            this.type = request.type.name.lowercase()
            this.partnership = request.partnershipId?.let { partnershipId ->
                PartnershipEntity.findById(partnershipId.toUUID())
                    ?: throw NotFoundException("Partnership with id $partnershipId not found")
            }
            this.headerAuth = request.headerAuth
        }

        webhookEntity.id.value
    }

    override fun getWebhooks(eventSlug: String): List<EventWebhook> = transaction {
        val eventEntity = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")

        EventWebhookEntity.find {
            EventWebhooksTable.eventId eq eventEntity.id
        }.map { webhookEntity ->
            EventWebhook(
                id = webhookEntity.id.value.toString(),
                url = webhookEntity.url,
                type = WebhookType.valueOf(webhookEntity.type.uppercase()),
                partnershipId = webhookEntity.partnership?.id?.value?.toString(),
                createdAt = webhookEntity.createdAt,
                updatedAt = webhookEntity.updatedAt,
            )
        }
    }

    override fun deleteWebhook(eventSlug: String, webhookId: UUID): Unit = transaction {
        val eventEntity = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")

        // Find webhook by both webhook ID and event ID to ensure it exists and belongs to the event
        val webhookEntity = EventWebhookEntity.find {
            (EventWebhooksTable.id eq webhookId) and (EventWebhooksTable.eventId eq eventEntity.id)
        }.singleOrNull()
            ?: throw NotFoundException("Webhook with id $webhookId not found for event $eventSlug")

        webhookEntity.delete()
    }
}
