package fr.devlille.partners.connect.integrations.domain

interface NotificationRepository {
    fun sendMessage(eventId: String, message: String)

    fun register(eventId: String, input: CreateIntegration): String
}
