package fr.devlille.partners.connect.notifications.domain

import fr.devlille.partners.connect.partnership.domain.DeliveryStatus
import kotlinx.serialization.Serializable

/**
 * Per-recipient delivery result for an email.
 *
 * @property value Recipient value
 * @property status Delivery status for this recipient
 */
@Serializable
data class RecipientResult(
    val value: String,
    val status: DeliveryStatus,
)
