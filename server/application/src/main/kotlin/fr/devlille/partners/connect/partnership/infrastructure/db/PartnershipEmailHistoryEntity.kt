package fr.devlille.partners.connect.partnership.infrastructure.db

import fr.devlille.partners.connect.users.infrastructure.db.UserEntity
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import java.util.UUID

/**
 * Exposed entity for partnership email history.
 *
 * Maps to partnership_email_history table.
 */
class PartnershipEmailHistoryEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<PartnershipEmailHistoryEntity>(
        PartnershipEmailHistoryTable,
    )

    var partnership by PartnershipEntity referencedOn
        PartnershipEmailHistoryTable.partnershipId
    var sentAt by PartnershipEmailHistoryTable.sentAt
    var senderEmail by PartnershipEmailHistoryTable.senderEmail
    var subject by PartnershipEmailHistoryTable.subject
    var bodyPlainText by PartnershipEmailHistoryTable.bodyPlainText
    var overallStatus by PartnershipEmailHistoryTable.overallStatus
    var triggeredBy by PartnershipEmailHistoryTable.triggeredBy
    var triggered by UserEntity referencedOn PartnershipEmailHistoryTable.triggeredBy

    /** Per-recipient delivery status records */
    val recipients by RecipientDeliveryStatusEntity referrersOn
        RecipientDeliveryStatusTable.emailHistoryId
}
