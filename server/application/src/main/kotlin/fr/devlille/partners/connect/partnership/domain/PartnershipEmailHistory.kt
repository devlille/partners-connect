package fr.devlille.partners.connect.partnership.domain

import fr.devlille.partners.connect.notifications.domain.RecipientResult
import fr.devlille.partners.connect.users.domain.User
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Partnership email history domain model.
 *
 * Represents a single email sent to a partnership with full audit trail including
 * sender, recipients, subject, body, and per-recipient delivery status.
 *
 * ## Use Cases
 * - **Audit Trail**: Complete record of all email communications with partners
 * - **Delivery Verification**: Per-recipient delivery status for debugging
 * - **Compliance**: Historical record preserved even if partnership or users are deleted
 * - **Content Review**: Full email body and subject stored for reference
 *
 * ## Database Constraints
 * - Email history survives partnership deletion (NO_ACTION foreign key)
 * - User reference preserved even if user is deleted (NO_ACTION foreign key)
 * - Body content stored in TEXT column (unlimited size)
 * - Indexed by (partnershipId, sentAt) for efficient chronological queries
 *
 * ## Delivery Status Rules
 * - **SENT**: All recipients successfully received the email
 * - **FAILED**: All recipients failed to receive the email
 * - **PARTIAL**: Mixed success/failure across multiple recipients
 *
 * @property id Unique identifier for this email history record
 * @property partnershipId UUID of the partnership this email was sent to
 * @property sentAt Timestamp when email was sent (UTC)
 * @property senderEmail Email address used as "From" address
 * @property subject Email subject line (max 500 characters in database)
 * @property bodyPlainText Email body content (stored as-is, HTML or plain text, unlimited size)
 * @property overallStatus Overall delivery status (SENT, FAILED, or PARTIAL)
 * @property triggeredBy User who triggered the email (organiser who performed the action)
 * @property recipients Per-recipient delivery results (at least one recipient required)
 */
@Serializable
data class PartnershipEmailHistory(
    val id: String,
    @SerialName("partnership_id")
    val partnershipId: String,
    @SerialName("sent_at")
    val sentAt: LocalDateTime,
    @SerialName("sender_email")
    val senderEmail: String,
    val subject: String,
    @SerialName("body_plain_text")
    val bodyPlainText: String,
    @SerialName("overall_status")
    val overallStatus: OverallDeliveryStatus,
    @SerialName("triggered_by")
    val triggeredBy: User,
    val recipients: List<RecipientResult>,
)
