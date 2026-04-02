package fr.devlille.partners.connect.partnership.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PartnershipQandaSummary(
    @SerialName("partnership_id")
    val partnershipId: String,
    @SerialName("company_name")
    val companyName: String,
    val questions: List<QandaQuestion>,
)
