package fr.devlille.partners.connect.partnership.infrastructure.db

import fr.devlille.partners.connect.partnership.domain.OverallDeliveryStatus
import fr.devlille.partners.connect.users.infrastructure.db.UsersTable
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.datetime

/**
 * Database table for partnership email history.
 *
 * Stores all emails sent to partnerships with full details including sender,
 * recipients, subject, body, and overall delivery status.
 */
object PartnershipEmailHistoryTable : UUIDTable("partnership_email_history") {
    /** Reference to partnership this email was sent to (preserved even if partnership deleted) */
    val partnershipId = reference(
        "partnership_id",
        PartnershipsTable,
        onDelete = ReferenceOption.NO_ACTION,
    )

    /** Timestamp when email was sent (UTC) */
    val sentAt = datetime("sent_at")
        .clientDefault { Clock.System.now().toLocalDateTime(TimeZone.UTC) }

    /** Email address used as "From" address */
    val senderEmail = varchar("sender_email", length = 255)

    /** Email subject line */
    val subject = varchar("subject", length = 500)

    /** Email body content stored as-is (HTML or plain text, unlimited size) */
    val bodyPlainText = text("body_plain_text")

    /** Overall delivery status (SENT, FAILED, or PARTIAL) */
    val overallStatus = enumeration<OverallDeliveryStatus>("overall_status")

    /** User ID of organiser who triggered the email, or "system" for automated emails */
    val triggeredBy = reference("triggered_by", UsersTable, onDelete = ReferenceOption.NO_ACTION)

    init {
        // Index for efficient chronological queries by partnership
        index(false, partnershipId, sentAt)
    }
}
