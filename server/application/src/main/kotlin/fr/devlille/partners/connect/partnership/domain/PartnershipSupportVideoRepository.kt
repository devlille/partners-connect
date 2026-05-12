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

    fun approve(videoId: UUID, reviewer: UserInfo): SupportVideoResponse

    fun decline(videoId: UUID, reviewer: UserInfo, reason: String? = null): SupportVideoResponse
}
