package fr.devlille.partners.connect.companies.domain

import fr.devlille.partners.connect.internal.infrastructure.api.PaginatedResponse
import java.util.UUID

/**
 * Repository for managing company job offer promotions.
 *
 * This repository handles the company-side operations for promoting job offers
 * at events. Companies can initiate promotions through their partnerships and
 * view the status of all promotions for their job offers.
 *
 * All methods throw appropriate exceptions (never return null or boolean flags):
 * - NotFoundException: Resource not found (404)
 * - ConflictException: Business rule violation (409)
 * - ForbiddenException: Insufficient permissions (403)
 * - BadRequestException: Invalid input (400)
 */
interface CompanyJobOfferPromotionRepository {
    /**
     * Promotes a job offer for visibility at an event through a partnership.
     *
     * **Business Rules**:
     * - Company must own the job offer (FR-001)
     * - Partnership must exist and belong to the company (FR-002)
     * - Event must not have ended (FR-030)
     * - Cannot create duplicate promotion if one exists with status PENDING or APPROVED (FR-003, FR-004)
     * - Can re-promote (upsert) if existing promotion has status DECLINED (FR-031, FR-019)
     *
     * **Side Effects**:
     * - Creates new promotion record with status=PENDING
     * - Or updates existing DECLINED promotion to status=PENDING
     * - Sends dual notifications (Mailjet email + Slack) to partnership contacts
     * - Sets promotedAt timestamp to current time
     *
     * @param companyId UUID of the company promoting the job offer
     * @param partnershipId UUID of the partnership through which to promote
     * @param jobOfferId UUID of the job offer to promote
     * @return UUID of the created or updated promotion
     * @throws NotFoundException if company, partnership, job offer, or event not found
     * @throws ForbiddenException if event has ended (FR-030)
     * @throws ConflictException if promotion already exists with status PENDING or APPROVED
     */
    fun promoteJobOffer(
        companyId: UUID,
        partnershipId: UUID,
        jobOfferId: UUID,
    ): UUID

    /**
     * Lists all promotions for a specific job offer owned by a company.
     *
     * **Filtering**:
     * - Optional partnershipId filter to show promotions for specific partnership
     * - Results are paginated using standard pagination parameters
     *
     * **Response Format**:
     * - Each promotion includes embedded job offer entity
     * - Uses event_slug (not event_id) per REST standards
     * - Ordered by promotedAt descending (newest first)
     *
     * @param companyId UUID of the company owning the job offer
     * @param jobOfferId UUID of the job offer
     * @param partnershipId Optional partnership filter (null = all partnerships)
     * @param page Page number (1-indexed)
     * @param pageSize Number of results per page (default 20, max 100)
     * @return Paginated response with promotion list and metadata
     * @throws NotFoundException if company or job offer not found
     */
    fun listJobOfferPromotions(
        companyId: UUID,
        jobOfferId: UUID,
        partnershipId: UUID? = null,
        page: Int = 1,
        pageSize: Int = 20,
    ): PaginatedResponse<JobOfferPromotionResponse>
}
