package fr.devlille.partners.connect.notifications.domain

import java.util.UUID

interface NotificationRepository {
    fun sendMessage(eventId: UUID, variables: NotificationVariables)
}
