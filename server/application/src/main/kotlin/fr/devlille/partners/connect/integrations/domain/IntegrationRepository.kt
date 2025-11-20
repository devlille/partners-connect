package fr.devlille.partners.connect.integrations.domain

import java.util.UUID

interface IntegrationRepository {
    fun register(
        eventSlug: String,
        provider: IntegrationProvider,
        usage: IntegrationUsage,
        input: CreateIntegration,
    ): UUID

    fun findByEvent(orgSlug: String, eventSlug: String): List<Integration>

    fun deleteById(orgSlug: String, eventSlug: String, integrationId: UUID)
}
