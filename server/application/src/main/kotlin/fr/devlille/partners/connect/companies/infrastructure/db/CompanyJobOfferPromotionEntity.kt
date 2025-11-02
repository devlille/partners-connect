package fr.devlille.partners.connect.companies.infrastructure.db

import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.internal.infrastructure.db.PromotionStatus
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEntity
import fr.devlille.partners.connect.users.infrastructure.db.UserEntity
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import org.jetbrains.exposed.v1.jdbc.SizedIterable
import java.util.UUID

/**
 * Exposed entity for company job offer promotions.
 *
 * Represents a promotion request for a job offer through a partnership,
 * with approval workflow tracking. This entity bridges the company's job offers
 * with event partnerships, enabling organizers to review and approve promotions.
 *
 * **Relationship Semantics**:
 * - jobOffer: References the job offer being promoted (CASCADE DELETE)
 * - partnership: References the partnership through which promotion occurs (CASCADE DELETE)
 * - event: Denormalized reference to the event for efficient querying (CASCADE DELETE)
 * - reviewedBy: Optional reference to the user who reviewed (SET NULL on delete)
 *
 * **Cascade Behavior**:
 * - Deleting a job offer → deletes all its promotions
 * - Deleting a partnership → deletes all promotions through that partnership
 * - Deleting an event → deletes all promotions for that event
 * - Deleting a reviewer user → sets reviewedBy to null (preserves audit trail)
 *
 * **State Transitions**:
 * - PENDING → APPROVED (via organizer approval)
 * - PENDING → DECLINED (via organizer decline)
 * - DECLINED → PENDING (via company re-promotion/upsert)
 * - APPROVED and DECLINED are terminal states (cannot transition further)
 *
 * **Index Strategy**:
 * - Composite index on (jobOfferId, partnershipId) for duplicate prevention
 * - Index on partnershipId for organizer queries
 * - Index on eventId for event-level queries
 * - Index on status for filtering pending reviews
 */
class CompanyJobOfferPromotionEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<CompanyJobOfferPromotionEntity>(CompanyJobOfferPromotionsTable) {
        fun listByPartnershipAndStatus(
            partnershipId: UUID,
            status: PromotionStatus?,
        ): SizedIterable<CompanyJobOfferPromotionEntity> {
            val baseQuery = CompanyJobOfferPromotionsTable.partnershipId eq partnershipId
            val query = if (status != null) {
                baseQuery and (CompanyJobOfferPromotionsTable.status eq status)
            } else {
                baseQuery
            }
            return find { query }.orderBy(CompanyJobOfferPromotionsTable.promotedAt to SortOrder.DESC)
        }

        fun listByEventAndStatus(
            eventId: UUID,
            status: PromotionStatus?,
        ): SizedIterable<CompanyJobOfferPromotionEntity> {
            val baseQuery = CompanyJobOfferPromotionsTable.eventId eq eventId
            val query = if (status != null) {
                baseQuery and (CompanyJobOfferPromotionsTable.status eq status)
            } else {
                baseQuery
            }
            return find { query }.orderBy(CompanyJobOfferPromotionsTable.promotedAt to SortOrder.ASC)
        }
    }

    /** The job offer being promoted (CASCADE DELETE) */
    var jobOffer by CompanyJobOfferEntity referencedOn CompanyJobOfferPromotionsTable.jobOfferId

    /** The partnership through which the offer is promoted (CASCADE DELETE) */
    var partnership by PartnershipEntity referencedOn CompanyJobOfferPromotionsTable.partnershipId

    /** Denormalized event reference for efficient querying (CASCADE DELETE) */
    var event by EventEntity referencedOn CompanyJobOfferPromotionsTable.eventId

    /** Current approval status: PENDING, APPROVED, or DECLINED */
    var status by CompanyJobOfferPromotionsTable.status

    /** When the company promoted this job offer (set on creation and re-promotion) */
    var promotedAt by CompanyJobOfferPromotionsTable.promotedAt

    /** When an organizer reviewed this promotion (null if still PENDING) */
    var reviewedAt by CompanyJobOfferPromotionsTable.reviewedAt

    /** User who approved or declined (null if still PENDING, SET NULL on user delete) */
    var reviewedBy by UserEntity optionalReferencedOn CompanyJobOfferPromotionsTable.reviewedBy

    /** Optional reason provided when declining (null for PENDING and APPROVED) */
    var declineReason by CompanyJobOfferPromotionsTable.declineReason

    /** Record creation timestamp (immutable) */
    val createdAt by CompanyJobOfferPromotionsTable.createdAt

    /** Last update timestamp (auto-updated on any modification) */
    var updatedAt by CompanyJobOfferPromotionsTable.updatedAt
}
