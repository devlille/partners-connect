package fr.devlille.partners.connect.events.domain

import fr.devlille.partners.connect.partnership.domain.PartnershipItem
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EventStats(
    val partners: List<PartnerStats>,
)

@Serializable
data class PartnerStats(
    val partnership: PartnershipItem,
    @SerialName("job_offers")
    val jobOffers: JobOfferStats,
    val activities: Int,
    val qanda: QandaStats,
    val tickets: Int,
    @SerialName("social_links")
    val socialLinks: Int,
    @SerialName("communication_plan")
    val communicationPlan: Int,
    val speakers: Int,
)

@Serializable
data class JobOfferStats(
    val total: Int,
    val validated: Int,
)

@Serializable
data class QandaStats(
    val questions: Int,
    val answers: Int,
)
