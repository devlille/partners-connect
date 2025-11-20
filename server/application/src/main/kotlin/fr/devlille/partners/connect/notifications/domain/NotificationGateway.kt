package fr.devlille.partners.connect.notifications.domain

import fr.devlille.partners.connect.integrations.domain.IntegrationProvider
import java.util.UUID

interface NotificationGateway {
    val provider: IntegrationProvider

    suspend fun send(integrationId: UUID, variables: NotificationVariables): Boolean
}
