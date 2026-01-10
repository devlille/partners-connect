package fr.devlille.partners.connect.notifications.domain

import fr.devlille.partners.connect.integrations.domain.IntegrationProvider
import fr.devlille.partners.connect.partnership.domain.PartnershipItem
import java.util.UUID

interface NotificationGateway {
    val provider: IntegrationProvider

    fun getDestination(eventId: UUID, partnership: PartnershipItem): Destination

    suspend fun send(integrationId: UUID, variables: NotificationVariables): DeliveryResult

    /**
     * Send a generic notification without template-based variables.
     *
     * This is a simpler alternative to send(integrationId, variables) for direct notifications
     * that don't require template lookups or complex variable substitution.
     *
     * @param integrationId UUID of the integration configuration (contains API credentials)
     * @param header Notification header/subject
     * @param body Notification body content (format depends on provider: HTML for email, markdown for Slack)
     * @param destination Destination object containing recipient details
     * @return DeliveryResult with overall status and per-recipient delivery status
     */
    suspend fun send(
        integrationId: UUID,
        header: String,
        body: String,
        destination: Destination,
    ): DeliveryResult
}

/**
 * Generic destination.
 */
interface Destination {
    val partnershipId: UUID
}
