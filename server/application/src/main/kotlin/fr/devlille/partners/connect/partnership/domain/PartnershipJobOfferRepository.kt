package fr.devlille.partners.connect.partnership.domain

import fr.devlille.partners.connect.auth.domain.UserInfo
import fr.devlille.partners.connect.companies.domain.JobOfferPromotionResponse
import fr.devlille.partners.connect.internal.infrastructure.api.PaginatedResponse
import fr.devlille.partners.connect.internal.infrastructure.db.PromotionStatus
import java.util.UUID

/**
 * Repository for managing partnership job offer operations from the organizer perspective.
 *
 * This repository handles organizer-side operations: viewing promoted job offers
 * for their events and approving/declining promotion requests. All endpoints using
 * this repository should be protected with AuthorizedOrganisationPlugin.
 *
 * All methods throw appropriate exceptions (never return null or boolean flags):
 * - NotFoundException: Resource not found (404)
 * - ConflictException: Invalid state transition (409)
 * - ForbiddenException: Insufficient permissions (403)
 */
interface PartnershipJobOfferRepository {
    /**
     * Lists all promoted job offers for a specific partnership.
     *
     * **Filtering**:
     * - Optional status filter (PENDING, APPROVED, DECLINED)
     * - Paginated results
     *
     * **Response Format**:
     * - Each promotion includes embedded job offer entity
     * - Uses event_slug per REST standards
     * - Ordered by promotedAt descending
     *
     * @param eventSlug Event slug
     * @param partnershipId Partnership UUID
     * @param status Optional promotion status filter
     * @param page Page number (1-indexed)
     * @param pageSize Results per page (default 20, max 100)
     * @return Paginated promotions for the partnership
     * @throws NotFoundException if organization, event, or partnership not found
     */
    fun listPartnershipJobOffers(
        eventSlug: String,
        partnershipId: UUID,
        status: PromotionStatus? = null,
        page: Int = 1,
        pageSize: Int = 20,
    ): PaginatedResponse<JobOfferPromotionResponse>

    /**
     * Lists all promoted job offers across all partnerships for an event.
     *
     * This endpoint is used by event organizers to view all promotion requests
     * requiring their review. Requires canEdit permission via AuthorizedOrganisationPlugin.
     *
     * **Filtering**:
     * - Optional status filter (typically PENDING for review queue)
     * - Paginated results
     *
     * **Response Format**:
     * - Each promotion includes embedded job offer entity
     * - Includes partnership_id to identify source of promotion
     * - Ordered by promotedAt ascending (oldest pending first)
     *
     * @param orgSlug Organization slug
     * @param eventSlug Event slug
     * @param status Optional promotion status filter
     * @param page Page number (1-indexed)
     * @param pageSize Results per page (default 20, max 100)
     * @return Paginated promotions for the event
     * @throws NotFoundException if organization or event not found
     */
    fun listEventJobOffers(
        orgSlug: String,
        eventSlug: String,
        status: PromotionStatus? = null,
        page: Int = 1,
        pageSize: Int = 20,
    ): PaginatedResponse<JobOfferPromotionResponse>

    /**
     * Approves a pending job offer promotion.
     *
     * **Business Rules**:
     * - Promotion must exist (FR-006)
     * - Status must be PENDING (FR-007)
     * - Cannot approve if already APPROVED or DECLINED
     *
     * **Side Effects**:
     * - Updates status to APPROVED
     * - Sets reviewedAt timestamp to current time
     * - Sets reviewedBy to the provided reviewer ID
     * - Sends approval notifications (Mailjet + Slack) to partnership contacts
     *
     * @param promotionId UUID of the promotion to approve
     * @param reviewer UserInfo of the organizer approving (from JWT)
     * @return Updated promotion with APPROVED status and review metadata
     * @throws NotFoundException if promotion not found
     * @throws ConflictException if promotion status is not PENDING
     */
    fun approvePromotion(
        promotionId: UUID,
        reviewer: UserInfo,
    ): JobOfferPromotionResponse

    /**
     * Declines a pending job offer promotion with optional reason.
     *
     * **Business Rules**:
     * - Promotion must exist (FR-009)
     * - Status must be PENDING (FR-010)
     * - Cannot decline if already APPROVED or DECLINED
     * - Reason is optional but recommended for partner feedback
     *
     * **Side Effects**:
     * - Updates status to DECLINED
     * - Sets reviewedAt timestamp to current time
     * - Sets reviewedBy to the provided reviewer ID
     * - Stores optional decline reason
     * - Sends decline notifications (Mailjet + Slack) including reason
     *
     * @param promotionId UUID of the promotion to decline
     * @param reviewer UserInfo of the organizer declining (from JWT)
     * @param reason Optional explanation for declining (max 500 chars recommended)
     * @return Updated promotion with DECLINED status, review metadata, and reason
     * @throws NotFoundException if promotion not found
     * @throws ConflictException if promotion status is not PENDING
     */
    fun declinePromotion(
        promotionId: UUID,
        reviewer: UserInfo,
        reason: String? = null,
    ): JobOfferPromotionResponse
}
