package fr.devlille.partners.connect.integrations.domain

import java.util.UUID

interface IntegrationRepository {
    fun register(eventSlug: String, usage: IntegrationUsage, input: CreateIntegration): UUID
}
