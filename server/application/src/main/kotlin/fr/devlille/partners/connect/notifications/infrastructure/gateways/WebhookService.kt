package fr.devlille.partners.connect.notifications.infrastructure.gateways

import fr.devlille.partners.connect.notifications.domain.NotificationVariables
import java.util.UUID

class WebhookService {
    fun sendWebhooks(eventId: UUID, variables: NotificationVariables): Boolean {
        // TODO: Implement webhook sending functionality
        return true
    }
}