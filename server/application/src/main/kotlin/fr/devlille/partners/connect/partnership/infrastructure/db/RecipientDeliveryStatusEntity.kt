package fr.devlille.partners.connect.partnership.infrastructure.db

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.java.UUIDEntity
import org.jetbrains.exposed.v1.dao.java.UUIDEntityClass
import java.util.UUID

/**
 * Exposed entity for recipient delivery status.
 *
 * Maps to recipient_delivery_status table.
 */
class RecipientDeliveryStatusEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<RecipientDeliveryStatusEntity>(RecipientDeliveryStatusTable)

    var emailHistory by PartnershipEmailHistoryEntity referencedOn
        RecipientDeliveryStatusTable.emailHistoryId
    var recipientEmail by RecipientDeliveryStatusTable.recipientEmail
    var deliveryStatus by RecipientDeliveryStatusTable.deliveryStatus
}
