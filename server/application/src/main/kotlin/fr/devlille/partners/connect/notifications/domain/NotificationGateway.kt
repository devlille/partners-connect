package fr.devlille.partners.connect.notifications.domain

import fr.devlille.partners.connect.integrations.domain.IntegrationProvider
import java.util.UUID

interface NotificationGateway {
    val provider: IntegrationProvider

    fun send(integrationId: UUID, message: String): Boolean
}
