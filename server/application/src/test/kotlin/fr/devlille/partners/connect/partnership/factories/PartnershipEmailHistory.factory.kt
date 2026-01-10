package fr.devlille.partners.connect.partnership.factories

import fr.devlille.partners.connect.partnership.domain.DeliveryStatus
import fr.devlille.partners.connect.partnership.domain.OverallDeliveryStatus
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEmailHistoryEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.RecipientDeliveryStatusEntity
import fr.devlille.partners.connect.users.infrastructure.db.UserEntity
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.util.UUID

/**
 * Factory function for creating mock partnership email history records in tests.
 *
 * Uses UUID-based defaults for unique fields and provides reasonable defaults for all parameters.
 * Does NOT manage transactions - caller must wrap in transaction block.
 *
 * @param id Unique identifier (default: random UUID)
 * @param partnershipId Reference to partnership (default: random UUID)
 * @param sentAt Timestamp when email was sent (default: current UTC time)
 * @param senderEmail "From" email address (default: UUID-based to ensure uniqueness)
 * @param subject Email subject line (default: UUID-based to ensure uniqueness)
 * @param bodyPlainText Email body content (default: simple test message)
 * @param overallStatus Overall delivery status (default: SENT)
 * @param triggeredBy User who triggered email (default: "system")
 * @param recipientEmails List of recipient emails for recipient status records (default: single mock email)
 * @param recipientStatuses Map of email to delivery status for recipients (default: all SENT)
 */
@Suppress("LongParameterList")
fun insertMockedPartnershipEmailHistory(
    id: UUID = UUID.randomUUID(),
    partnershipId: UUID = UUID.randomUUID(),
    sentAt: LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.UTC),
    senderEmail: String = "no-reply-$id@devlille.fr",
    subject: String = "Test Subject $id",
    bodyPlainText: String = "Test email body for email history $id",
    overallStatus: OverallDeliveryStatus = OverallDeliveryStatus.SENT,
    triggeredBy: UUID = UUID.randomUUID(),
    recipientEmails: List<String> = listOf("partner@company.com"),
    recipientStatuses: Map<String, DeliveryStatus> = recipientEmails.associateWith { DeliveryStatus.SENT },
): PartnershipEmailHistoryEntity {
    val emailHistory = PartnershipEmailHistoryEntity.new(id) {
        this.partnership = PartnershipEntity[partnershipId]
        this.sentAt = sentAt
        this.senderEmail = senderEmail
        this.subject = subject
        this.bodyPlainText = bodyPlainText
        this.overallStatus = overallStatus
        this.triggered = UserEntity[triggeredBy]
    }

    // Create recipient delivery status records
    recipientEmails.forEach { email ->
        RecipientDeliveryStatusEntity.new {
            this.emailHistory = emailHistory
            this.recipientEmail = email
            this.deliveryStatus = recipientStatuses[email] ?: DeliveryStatus.SENT
        }
    }

    return emailHistory
}
