package fr.devlille.partners.connect.partnership.infrastructure.db

import fr.devlille.partners.connect.partnership.domain.DeliveryStatus
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable

/**
 * Database table for per-recipient delivery status.
 *
 * Tracks delivery status for each recipient of an email, enabling
 * per-recipient granularity for emails with multiple recipients.
 */
object RecipientDeliveryStatusTable : UUIDTable("recipient_delivery_status") {
    /** Reference to the email history record this status belongs to */
    val emailHistoryId = reference(
        "email_history_id",
        PartnershipEmailHistoryTable,
        onDelete = ReferenceOption.CASCADE,
    )

    /** Recipient email address */
    val recipientEmail = varchar("recipient_email", length = 255)

    /** Delivery status for this recipient (SENT or FAILED) */
    val deliveryStatus = enumeration<DeliveryStatus>("delivery_status")

    init {
        // Index for efficient joins and queries
        index(false, emailHistoryId)
        // Index for querying by recipient email
        index(false, recipientEmail)
    }
}
