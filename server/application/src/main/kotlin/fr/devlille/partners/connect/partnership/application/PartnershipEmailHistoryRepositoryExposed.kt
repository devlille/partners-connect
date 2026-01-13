package fr.devlille.partners.connect.partnership.application

import fr.devlille.partners.connect.internal.infrastructure.api.PaginatedResponse
import fr.devlille.partners.connect.internal.infrastructure.api.paginated
import fr.devlille.partners.connect.notifications.domain.DeliveryResult
import fr.devlille.partners.connect.notifications.domain.RecipientResult
import fr.devlille.partners.connect.partnership.domain.PartnershipEmailHistory
import fr.devlille.partners.connect.partnership.domain.PartnershipEmailHistoryRepository
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEmailHistoryEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEmailHistoryTable
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.RecipientDeliveryStatusEntity
import fr.devlille.partners.connect.users.application.toDomain
import fr.devlille.partners.connect.users.infrastructure.db.UserEntity
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

/**
 * Exposed ORM implementation of PartnershipEmailHistoryRepository.
 *
 * Handles creation and retrieval of partnership email history records
 * with per-recipient delivery status tracking.
 */
class PartnershipEmailHistoryRepositoryExposed : PartnershipEmailHistoryRepository {
    override fun create(
        partnershipId: UUID,
        senderEmail: String,
        subject: String,
        bodyPlainText: String,
        deliveryResult: DeliveryResult,
        triggeredBy: UUID,
    ): PartnershipEmailHistory = transaction {
        // Create email history record
        val historyEntity = PartnershipEmailHistoryEntity.new {
            this.partnership = PartnershipEntity[partnershipId]
            this.senderEmail = senderEmail
            this.subject = subject
            this.bodyPlainText = bodyPlainText
            this.overallStatus = deliveryResult.overallStatus
            this.triggered = UserEntity[triggeredBy]
        }

        // Create per-recipient delivery status records
        deliveryResult.recipients.forEach { recipient ->
            RecipientDeliveryStatusEntity.new {
                this.emailHistory = historyEntity
                this.recipientEmail = recipient.value
                this.deliveryStatus = recipient.status
            }
        }

        // Return domain model
        historyEntity.toDomain()
    }

    override fun findByPartnershipId(
        partnershipId: UUID,
        page: Int,
        pageSize: Int,
    ): PaginatedResponse<PartnershipEmailHistory> = transaction {
        val emailHistory = PartnershipEmailHistoryEntity
            .find { PartnershipEmailHistoryTable.partnershipId eq partnershipId }
            .orderBy(PartnershipEmailHistoryTable.sentAt to SortOrder.DESC)
            .paginated(page, pageSize)
        PaginatedResponse(
            items = emailHistory.map { it.toDomain() },
            page = page,
            pageSize = pageSize,
            total = PartnershipEmailHistoryEntity
                .find { PartnershipEmailHistoryTable.partnershipId eq partnershipId }
                .count(),
        )
    }
}

/**
 * Extension function to convert PartnershipEmailHistoryEntity to domain model.
 */
fun PartnershipEmailHistoryEntity.toDomain(): PartnershipEmailHistory =
    PartnershipEmailHistory(
        id = id.value.toString(),
        partnershipId = partnership.id.value.toString(),
        sentAt = sentAt,
        senderEmail = senderEmail,
        subject = subject,
        bodyPlainText = bodyPlainText,
        overallStatus = overallStatus,
        triggeredBy = triggered.toDomain(),
        recipients = recipients.map { it.toDomain() },
    )

/**
 * Extension function to convert RecipientDeliveryStatusEntity to RecipientResult.
 */
fun RecipientDeliveryStatusEntity.toDomain(): RecipientResult =
    RecipientResult(value = recipientEmail, status = deliveryStatus)
