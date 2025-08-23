package fr.devlille.partners.connect.webhooks.domain

import java.util.UUID

interface WebhookRepository {
    fun createWebhook(eventSlug: String, request: CreateEventWebhookRequest): UUID

    fun getWebhooks(eventSlug: String): List<EventWebhook>

    fun deleteWebhook(eventSlug: String, webhookId: UUID)
}
