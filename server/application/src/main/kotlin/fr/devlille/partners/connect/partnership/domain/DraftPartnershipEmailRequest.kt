package fr.devlille.partners.connect.partnership.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DraftPartnershipEmailRequest(
    @SerialName("partnership_ids")
    val partnershipIds: List<String>,
    val prompt: String,
)
