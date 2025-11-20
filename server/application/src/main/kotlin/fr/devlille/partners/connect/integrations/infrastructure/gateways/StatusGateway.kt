package fr.devlille.partners.connect.integrations.infrastructure.gateways

import fr.devlille.partners.connect.integrations.domain.IntegrationProvider
import java.util.UUID

interface StatusGateway {
    val provider: IntegrationProvider

    suspend fun status(integrationId: UUID): Boolean
}
