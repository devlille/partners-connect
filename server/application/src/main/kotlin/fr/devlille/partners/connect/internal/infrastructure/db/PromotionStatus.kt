package fr.devlille.partners.connect.internal.infrastructure.db

/**
 * Represents the lifecycle status of a job offer promotion.
 *
 * Status transitions:
 * - PENDING: Initial state when promotion is submitted, or after re-promotion of a DECLINED offer
 * - APPROVED: Terminal state when organizer accepts the promotion
 * - DECLINED: Promotion rejected by organizer, can transition back to PENDING via re-promotion
 */
enum class PromotionStatus {
    /**
     * Awaiting organizer review.
     * Can transition to APPROVED or DECLINED.
     */
    PENDING,

    /**
     * Accepted by event organizer.
     * Terminal state - no further transitions allowed.
     */
    APPROVED,

    /**
     * Rejected by event organizer.
     * Can be re-promoted to return to PENDING status.
     */
    DECLINED,
}
