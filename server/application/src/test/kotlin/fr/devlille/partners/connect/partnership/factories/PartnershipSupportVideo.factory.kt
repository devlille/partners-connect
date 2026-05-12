package fr.devlille.partners.connect.partnership.factories

import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.internal.infrastructure.db.PromotionStatus
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipSupportVideoEntity
import fr.devlille.partners.connect.users.infrastructure.db.UserEntity
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.util.UUID

@Suppress("LongParameterList")
fun insertMockedSupportVideo(
    partnershipId: UUID,
    eventId: UUID,
    id: UUID = UUID.randomUUID(),
    url: String = "https://storage.googleapis.com/test-bucket/events/$eventId/partnerships/$partnershipId/support-video.mp4",
    status: PromotionStatus = PromotionStatus.PENDING,
    reviewerUserId: UUID? = null,
    declineReason: String? = null,
): PartnershipSupportVideoEntity = PartnershipSupportVideoEntity.new(id) {
    this.partnership = PartnershipEntity[partnershipId]
    this.event = EventEntity[eventId]
    this.url = url
    this.status = status
    this.reviewedAt = if (status == PromotionStatus.PENDING || reviewerUserId == null) {
        null
    } else {
        Clock.System.now().toLocalDateTime(TimeZone.UTC)
    }
    this.reviewedBy = reviewerUserId?.let { UserEntity[it] }
    this.declineReason = declineReason
}
