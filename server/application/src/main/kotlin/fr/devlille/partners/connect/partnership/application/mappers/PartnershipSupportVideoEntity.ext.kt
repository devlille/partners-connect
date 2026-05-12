package fr.devlille.partners.connect.partnership.application.mappers

import fr.devlille.partners.connect.partnership.domain.SupportVideoResponse
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipSupportVideoEntity

internal fun PartnershipSupportVideoEntity.toDomain(): SupportVideoResponse = SupportVideoResponse(
    id = id.value.toString(),
    partnershipId = partnership.id.value.toString(),
    eventSlug = event.slug,
    url = url,
    status = status,
    submittedAt = submittedAt,
    reviewedAt = reviewedAt,
    reviewedBy = reviewedBy?.id?.value?.toString(),
    declineReason = declineReason,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
