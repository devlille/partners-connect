package fr.devlille.partners.connect.companies.domain

import fr.devlille.partners.connect.internal.infrastructure.db.PromotionStatus
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request to promote a job offer for visibility at an event.
 *
 * This request initiates the promotion workflow by creating a pending promotion
 * that requires organizer approval before the job offer appears in the event hall.
 *
 * @property jobOfferId UUID string of the job offer to promote
 */
@Serializable
data class PromoteJobOfferRequest(
    @SerialName("job_offer_id")
    val jobOfferId: String,
)

/**
 * Embedded job offer data within promotion response.
 *
 * This simplified representation contains only the essential fields needed
 * to display job offer information in the promotion context.
 *
 * @property id Job offer identifier
 * @property title Job offer title
 * @property url External URL to the full job offer page
 */
@Serializable
data class JobOffer(
    val id: String,
    val title: String,
    val url: String,
)

/**
 * Response containing complete job offer promotion details.
 *
 * Includes the promotion metadata, current status, review information, and the embedded
 * job offer entity. The event_slug field (not event_id) follows REST best practices
 * for human-readable resource references.
 *
 * @property id Unique promotion identifier
 * @property jobOfferId ID of the promoted job offer
 * @property partnershipId Partnership through which promotion was requested
 * @property eventSlug Human-readable event identifier (not event_id per standards)
 * @property status Current promotion status (PENDING, APPROVED, DECLINED)
 * @property promotedAt Timestamp when promotion was initially requested
 * @property reviewedAt Timestamp when organizer approved/declined (null if pending)
 * @property reviewedBy User ID of organizer who reviewed (null if pending)
 * @property declineReason Optional reason provided when declining promotion
 * @property jobOffer Complete job offer entity embedded for convenience
 * @property createdAt Record creation timestamp
 * @property updatedAt Record last modification timestamp
 */
@Serializable
data class JobOfferPromotionResponse(
    val id: String,
    @SerialName("job_offer_id")
    val jobOfferId: String,
    @SerialName("partnership_id")
    val partnershipId: String,
    @SerialName("event_slug")
    val eventSlug: String,
    val status: PromotionStatus,
    @SerialName("promoted_at")
    val promotedAt: LocalDateTime,
    @SerialName("reviewed_at")
    val reviewedAt: LocalDateTime? = null,
    @SerialName("reviewed_by")
    val reviewedBy: String? = null,
    @SerialName("decline_reason")
    val declineReason: String? = null,
    @SerialName("job_offer")
    val jobOffer: JobOffer,
    @SerialName("created_at")
    val createdAt: LocalDateTime,
    @SerialName("updated_at")
    val updatedAt: LocalDateTime,
)
