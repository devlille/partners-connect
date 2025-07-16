package fr.devlille.partners.connect.integrations.domain

import java.util.UUID

interface NotificationGateway {
    val provider: IntegrationProvider

    fun send(integrationId: UUID, message: String): Boolean
}
