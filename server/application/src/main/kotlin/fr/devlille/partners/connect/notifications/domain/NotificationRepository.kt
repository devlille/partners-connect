package fr.devlille.partners.connect.notifications.domain

interface NotificationRepository {
    suspend fun sendMessage(eventSlug: String, variables: NotificationVariables)
}
