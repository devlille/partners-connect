package fr.devlille.partners.connect.partnership.infrastructure.db

import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.internal.infrastructure.db.PromotionStatus
import fr.devlille.partners.connect.users.infrastructure.db.UserEntity
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import org.jetbrains.exposed.v1.jdbc.SizedIterable
import java.util.UUID

class PartnershipSupportVideoEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<PartnershipSupportVideoEntity>(PartnershipSupportVideosTable) {
        fun singleByPartnership(partnershipId: UUID): PartnershipSupportVideoEntity? =
            find { PartnershipSupportVideosTable.partnershipId eq partnershipId }.singleOrNull()

        fun listByEventAndStatus(
            eventId: UUID,
            status: PromotionStatus?,
        ): SizedIterable<PartnershipSupportVideoEntity> {
            val base = PartnershipSupportVideosTable.eventId eq eventId
            val query = if (status != null) base and (PartnershipSupportVideosTable.status eq status) else base
            return find { query }.orderBy(PartnershipSupportVideosTable.submittedAt to SortOrder.ASC)
        }
    }

    var partnership by PartnershipEntity referencedOn PartnershipSupportVideosTable.partnershipId
    var event by EventEntity referencedOn PartnershipSupportVideosTable.eventId
    var url by PartnershipSupportVideosTable.url
    var status by PartnershipSupportVideosTable.status
    var submittedAt by PartnershipSupportVideosTable.submittedAt
    var reviewedAt by PartnershipSupportVideosTable.reviewedAt
    var reviewedBy by UserEntity optionalReferencedOn PartnershipSupportVideosTable.reviewedBy
    var declineReason by PartnershipSupportVideosTable.declineReason
    val createdAt by PartnershipSupportVideosTable.createdAt
    var updatedAt by PartnershipSupportVideosTable.updatedAt
}
