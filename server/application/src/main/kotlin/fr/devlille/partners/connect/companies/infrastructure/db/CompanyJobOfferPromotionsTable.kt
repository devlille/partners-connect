package fr.devlille.partners.connect.companies.infrastructure.db

import fr.devlille.partners.connect.events.infrastructure.db.EventsTable
import fr.devlille.partners.connect.internal.infrastructure.db.PromotionStatus
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipsTable
import fr.devlille.partners.connect.users.infrastructure.db.UsersTable
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.datetime

/**
 * Database table for company job offer promotions.
 *
 * Tracks when companies promote their job offers through event partnerships,
 * including approval workflow status and timestamps. This table implements a
 * three-way relationship between job offers, partnerships, and events with
 * an approval workflow overlay.
 *
 * **Schema Design**:
 * - Primary key: UUID auto-generated
 * - Foreign keys: job_offer_id, partnership_id, event_id, reviewed_by (optional)
 * - Unique constraint: (job_offer_id, partnership_id) prevents duplicates
 * - Status enum: PENDING, APPROVED, DECLINED (string column, max 20 chars)
 * - All timestamps in UTC using kotlinx.datetime
 *
 * **Cascade Behavior**:
 * - jobOfferId: CASCADE DELETE (promotions are owned by job offers)
 *   Rationale: When a company deletes a job offer, all promotions should be removed
 * - partnershipId: CASCADE DELETE (promotions belong to partnership context)
 *   Rationale: When a partnership is deleted, its promotions are no longer valid
 * - eventId: CASCADE DELETE (promotions are event-specific)
 *   Rationale: When an event is deleted, all associated promotions should be cleaned up
 * - reviewedBy: SET NULL (preserve audit trail even if reviewer is deleted)
 *   Rationale: Historical data integrity - we keep the review record but lose reviewer identity
 *
 * **Index Strategy**:
 * 1. Non-unique index on jobOfferId
 *    - Usage: Company views all promotions for their job offer
 *    - Query: SELECT * FROM promotions WHERE job_offer_id = ?
 *    - Selectivity: Low (each offer may have multiple promotions across events)
 *
 * 2. Non-unique index on partnershipId
 *    - Usage: Organizer views promotions for their partnership
 *    - Query: SELECT * FROM promotions WHERE partnership_id = ?
 *    - Selectivity: Medium (each partnership may have multiple job offer promotions)
 *
 * 3. Composite non-unique index on (eventId, status)
 *    - Usage: Organizer views pending promotions for event review queue
 *    - Query: SELECT * FROM promotions WHERE event_id = ? AND status = 'PENDING'
 *    - Selectivity: High (combines event scope with status filter)
 *    - Ordering: Supports efficient filtering and sorting
 *
 * 4. Unique composite index on (jobOfferId, partnershipId)
 *    - Usage: Prevents duplicate promotion requests
 *    - Business rule: One promotion per job offer per partnership at any time
 *    - Side effect: Enables efficient upsert pattern for re-promotion (FR-031)
 *
 * **Column Semantics**:
 * - promotedAt: Set on creation and reset on re-promotion (not immutable)
 * - reviewedAt: NULL until status changes from PENDING
 * - reviewedBy: NULL for PENDING, set on approval/decline
 * - declineReason: Only populated when status=DECLINED
 * - createdAt: Immutable record creation timestamp
 * - updatedAt: Auto-updated on any modification for audit trail
 *
 * **Query Patterns**:
 * 1. Company lists promotions: WHERE job_offer_id = ? [uses index #1]
 * 2. Organizer lists partnership offers: WHERE partnership_id = ? [uses index #2]
 * 3. Organizer review queue: WHERE event_id = ? AND status = 'PENDING' [uses index #3]
 * 4. Duplicate check: WHERE job_offer_id = ? AND partnership_id = ? [uses unique index #4]
 */
object CompanyJobOfferPromotionsTable : UUIDTable("company_job_offer_promotions") {
    /** Foreign key to job offer being promoted (CASCADE DELETE) */
    val jobOfferId = reference("job_offer_id", CompanyJobOfferTable)

    /** Foreign key to partnership through which offer is promoted (CASCADE DELETE) */
    val partnershipId = reference("partnership_id", PartnershipsTable)

    /** Denormalized event reference for efficient filtering (CASCADE DELETE) */
    val eventId = reference("event_id", EventsTable)

    /** Current workflow status: PENDING, APPROVED, or DECLINED */
    val status = enumerationByName<PromotionStatus>("status", length = 20)

    /** When company promoted/re-promoted this offer (updated on re-promotion) */
    val promotedAt = datetime("promoted_at").clientDefault {
        Clock.System.now().toLocalDateTime(TimeZone.UTC)
    }

    /** When organizer reviewed (null for PENDING) */
    val reviewedAt = datetime("reviewed_at").nullable()

    /** User who reviewed (null for PENDING, SET NULL on user delete) */
    val reviewedBy = reference("reviewed_by", UsersTable).nullable()

    /** Optional decline explanation (null unless DECLINED) */
    val declineReason = text("decline_reason").nullable()

    /** Immutable creation timestamp */
    val createdAt = datetime("created_at").clientDefault {
        Clock.System.now().toLocalDateTime(TimeZone.UTC)
    }

    /** Auto-updated modification timestamp */
    val updatedAt = datetime("updated_at").clientDefault {
        Clock.System.now().toLocalDateTime(TimeZone.UTC)
    }

    init {
        // Index #1: Company queries all promotions for their job offer
        index(isUnique = false, jobOfferId)

        // Index #2: Organizer queries all promotions for their partnership
        index(isUnique = false, partnershipId)

        // Index #3: Organizer filters event promotions by status (e.g., pending review queue)
        index(isUnique = false, eventId, status)

        // Index #4: Unique constraint prevents duplicate promotions + enables upsert
        uniqueIndex(jobOfferId, partnershipId)
    }
}
