package fr.devlille.partners.connect.integrations.domain

import java.util.UUID

interface IntegrationRegistrar<T : CreateIntegration> {
    val supportedUsages: Set<IntegrationUsage>

    fun register(eventId: UUID, usage: IntegrationUsage, input: T): UUID

    fun unregister(integrationId: UUID)

    fun supports(input: CreateIntegration): Boolean
}
