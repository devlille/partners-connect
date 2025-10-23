package fr.devlille.partners.connect.companies.factories

import fr.devlille.partners.connect.companies.infrastructure.db.CompanyJobOfferEntity
import fr.devlille.partners.connect.companies.infrastructure.db.CompanyJobOfferPromotionEntity
import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.internal.infrastructure.db.PromotionStatus
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEntity
import fr.devlille.partners.connect.users.infrastructure.db.UserEntity
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.util.UUID

@Suppress("LongParameterList")
fun insertMockCompanyJobOfferPromotion(
    jobOfferId: UUID,
    partnershipId: UUID,
    eventId: UUID,
    id: UUID = UUID.randomUUID(),
    status: PromotionStatus = PromotionStatus.PENDING,
    userReviewId: UUID? = null,
    declineReason: String? = null,
): CompanyJobOfferPromotionEntity {
    return CompanyJobOfferPromotionEntity.new(id) {
        jobOffer = CompanyJobOfferEntity[jobOfferId]
        partnership = PartnershipEntity[partnershipId]
        event = EventEntity[eventId]
        this.status = status
        this.reviewedAt = if (status == PromotionStatus.PENDING || userReviewId == null) {
            null
        } else {
            Clock.System.now().toLocalDateTime(TimeZone.UTC)
        }
        this.reviewedBy = userReviewId?.let { UserEntity[it] }
        this.declineReason = declineReason
    }
}
