package fr.devlille.partners.connect.notifications.domain

interface NotificationRepository {
    fun sendMessage(eventId: String, message: String)
}
