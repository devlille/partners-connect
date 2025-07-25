package fr.devlille.partners.connect.integrations.domain

import java.util.UUID

interface IntegrationRepository {
    fun register(eventId: UUID, usage: IntegrationUsage, input: CreateIntegration): UUID
}
