package fr.devlille.partners.connect.partnership.domain

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CommunicationItem(
    @SerialName("partnership_id")
    val partnershipId: String,
    @SerialName("company_name")
    val companyName: String,
    @SerialName("publication_date")
    val publicationDate: LocalDateTime? = null,
    @SerialName("support_url")
    val supportUrl: String? = null,
)

@Serializable
data class CommunicationPlan(
    val done: List<CommunicationItem>,
    val planned: List<CommunicationItem>,
    val unplanned: List<CommunicationItem>,
)
