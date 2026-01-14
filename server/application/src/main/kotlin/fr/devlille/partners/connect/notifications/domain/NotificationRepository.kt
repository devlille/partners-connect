package fr.devlille.partners.connect.notifications.domain

interface NotificationRepository {
    suspend fun sendMessage(variables: NotificationVariables): List<DeliveryResult>

    /**
     * @param eventSlug The event slug to identify the mailing integration
     * @param destination Destination object containing From, To, and optional CC contacts
     * @param subject Email subject line (will be prefixed with [event_name])
     * @param htmlBody Email body content in HTML format
     * @throws NotFoundException if no Mailjet integration is configured for the event
     */
    suspend fun sendMessage(
        eventSlug: String,
        destination: Destination,
        subject: String,
        htmlBody: String,
    ): DeliveryResult
}
