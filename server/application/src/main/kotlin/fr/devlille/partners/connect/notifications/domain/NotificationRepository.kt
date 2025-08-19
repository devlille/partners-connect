package fr.devlille.partners.connect.notifications.domain

interface NotificationRepository {
    fun sendMessage(eventSlug: String, variables: NotificationVariables)
}
