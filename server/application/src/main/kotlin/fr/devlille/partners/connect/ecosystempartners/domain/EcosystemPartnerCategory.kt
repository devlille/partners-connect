package fr.devlille.partners.connect.ecosystempartners.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EcosystemPartnerCategory(
    val id: String,
    @SerialName("event_id")
    val eventId: String,
    val name: String,
    @SerialName("display_order")
    val displayOrder: Int? = null,
)
