package fr.devlille.partners.connect.partnership.domain

import fr.devlille.partners.connect.internal.infrastructure.api.PaginatedResponse
import fr.devlille.partners.connect.notifications.domain.DeliveryResult
import java.util.UUID

/**
 * Repository interface for partnership email history operations.
 *
 * Provides methods to create and retrieve email history records for partnerships.
 *
 * ## Implementation Notes
 * - Repository does NOT depend on other repositories (single responsibility)
 * - All email content is stored verbatim (no sanitization or validation in repository)
 * - Foreign key constraints preserved (NO_ACTION) to maintain audit trail
 * - Pagination returns newest emails first (ORDER BY sentAt DESC)
 *
 * ## Transaction Handling
 * - All methods must be called within a database transaction context
 * - No transaction management performed by repository itself
 * - Caller responsible for commit/rollback logic
 */
interface PartnershipEmailHistoryRepository {
    /**
     * Create a new email history record.
     *
     * ## Validation Expectations
     * - Subject length validated by caller (max 500 chars in database)
     * - At least one recipient must be in deliveryResult
     * - Email addresses validated by caller (format)
     * - Partnership existence NOT validated (supports historical data)
     *
     * @param partnershipId UUID of the partnership that received the email
     * @param senderEmail Email address used as "From" address
     * @param subject Email subject line (max 500 characters)
     * @param bodyPlainText Email body content (HTML or plain text, unlimited size)
     * @param deliveryResult Delivery result with overall status and per-recipient delivery status
     * @param triggeredBy User UUID of organiser who triggered the email (foreign key to UsersTable)
     * @return Created PartnershipEmailHistory domain model
     * @throws Exception if transaction fails or database constraint violated
     */
    @Suppress("LongParameterList")
    fun create(
        partnershipId: UUID,
        senderEmail: String,
        subject: String,
        bodyPlainText: String,
        deliveryResult: DeliveryResult,
        triggeredBy: UUID,
    ): PartnershipEmailHistory

    /**
     * Find email history for a partnership with pagination.
     *
     * ## Pagination Rules
     * - Page numbers are 0-indexed (page=0 is first page)
     * - Returns empty list if page exceeds total pages
     * - Results ordered newest-first (DESC by sentAt)
     * - Default pageSize is 20 (max recommended: 100)
     *
     * @param partnershipId UUID of the partnership to retrieve email history for
     * @param page Zero-based page number (default 0, minimum 0)
     * @param pageSize Number of records per page (default 20, minimum 1)
     * @return List of email history records (newest first, may be empty)
     */
    fun findByPartnershipId(
        partnershipId: UUID,
        page: Int = 0,
        pageSize: Int = 20,
    ): PaginatedResponse<PartnershipEmailHistory>
}
