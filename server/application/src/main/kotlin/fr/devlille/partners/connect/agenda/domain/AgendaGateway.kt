package fr.devlille.partners.connect.agenda.domain

import fr.devlille.partners.connect.integrations.domain.IntegrationProvider
import java.util.UUID

interface AgendaGateway {
    val provider: IntegrationProvider

    fun fetchAndStore(integrationId: UUID, eventId: UUID)
}
