package fr.devlille.partners.connect.partnership.domain

import fr.devlille.partners.connect.internal.infrastructure.db.PromotionStatus
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeclineSupportVideoRequest(
    val reason: String? = null,
)

@Serializable
data class SupportVideoResponse(
    val id: String,
    @SerialName("partnership_id") val partnershipId: String,
    @SerialName("event_slug") val eventSlug: String,
    val url: String,
    val status: PromotionStatus,
    @SerialName("submitted_at") val submittedAt: LocalDateTime,
    @SerialName("reviewed_at") val reviewedAt: LocalDateTime?,
    @SerialName("reviewed_by") val reviewedBy: String?,
    @SerialName("decline_reason") val declineReason: String?,
    @SerialName("created_at") val createdAt: LocalDateTime,
    @SerialName("updated_at") val updatedAt: LocalDateTime,
)
