package fr.devlille.partners.connect.partnership.application.mappers

import fr.devlille.partners.connect.companies.domain.JobOffer
import fr.devlille.partners.connect.companies.domain.JobOfferPromotionResponse
import fr.devlille.partners.connect.companies.infrastructure.db.CompanyJobOfferPromotionEntity

fun CompanyJobOfferPromotionEntity.toDomain(): JobOfferPromotionResponse = JobOfferPromotionResponse(
    id = id.value.toString(),
    jobOfferId = jobOffer.id.value.toString(),
    partnershipId = partnership.id.value.toString(),
    eventSlug = event.slug,
    status = status,
    promotedAt = promotedAt,
    reviewedAt = reviewedAt,
    reviewedBy = reviewedBy?.id?.value?.toString(),
    declineReason = declineReason,
    jobOffer = JobOffer(
        id = jobOffer.id.value.toString(),
        title = jobOffer.title,
        url = jobOffer.url,
    ),
    createdAt = createdAt,
    updatedAt = updatedAt,
)
