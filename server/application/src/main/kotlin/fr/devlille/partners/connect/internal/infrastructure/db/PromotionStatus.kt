package fr.devlille.partners.connect.internal.infrastructure.db

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents the lifecycle status of a job offer promotion.
 *
 * Status transitions:
 * - PENDING: Initial state when promotion is submitted, or after re-promotion of a DECLINED offer
 * - APPROVED: Terminal state when organizer accepts the promotion
 * - DECLINED: Promotion rejected by organizer, can transition back to PENDING via re-promotion
 *
 * Serialization: emitted as lowercase to match the OpenAPI contract. The Exposed table column
 * stores `enumerationByName` which uses the enum's `.name` (uppercase), so the database
 * representation is unaffected by these @SerialName annotations.
 */
@Serializable
enum class PromotionStatus {
    /**
     * Awaiting organizer review.
     * Can transition to APPROVED or DECLINED.
     */
    @SerialName("pending")
    PENDING,

    /**
     * Accepted by event organizer.
     * Terminal state - no further transitions allowed.
     */
    @SerialName("approved")
    APPROVED,

    /**
     * Rejected by event organizer.
     * Can be re-promoted to return to PENDING status.
     */
    @SerialName("declined")
    DECLINED,
}
