package fr.devlille.partners.connect.partnership.domain

import fr.devlille.partners.connect.auth.domain.UserInfo
import fr.devlille.partners.connect.internal.infrastructure.api.PaginatedResponse
import fr.devlille.partners.connect.internal.infrastructure.db.PromotionStatus
import java.util.UUID

interface PartnershipSupportVideoRepository {
    fun listEventSupportVideos(
        orgSlug: String,
        eventSlug: String,
        status: PromotionStatus? = null,
        page: Int = 1,
        pageSize: Int = 20,
    ): PaginatedResponse<SupportVideoResponse, Unit>

    /**
     * Approves a pending support video.
     *
     * Throws [io.ktor.server.plugins.NotFoundException] when the event does not exist or when the video does not
     * exist or does not belong to the event/partnership scope identified by [eventSlug] and [partnershipId] (404).
     */
    fun approve(
        eventSlug: String,
        partnershipId: UUID,
        videoId: UUID,
        reviewer: UserInfo,
    ): SupportVideoResponse

    /**
     * Declines a pending support video.
     *
     * Throws [io.ktor.server.plugins.NotFoundException] when the event does not exist or when the video does not
     * exist or does not belong to the event/partnership scope identified by [eventSlug] and [partnershipId] (404).
     */
    fun decline(
        eventSlug: String,
        partnershipId: UUID,
        videoId: UUID,
        reviewer: UserInfo,
        reason: String? = null,
    ): SupportVideoResponse
}
