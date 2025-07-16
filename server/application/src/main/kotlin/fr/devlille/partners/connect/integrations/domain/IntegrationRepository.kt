package fr.devlille.partners.connect.integrations.domain

interface IntegrationRepository {
    fun register(eventId: String, usage: IntegrationUsage, input: CreateIntegration): String
}
