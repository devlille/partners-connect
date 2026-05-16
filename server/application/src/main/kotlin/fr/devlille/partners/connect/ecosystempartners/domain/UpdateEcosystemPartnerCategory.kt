package fr.devlille.partners.connect.ecosystempartners.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UpdateEcosystemPartnerCategory(
    val name: String? = null,
    @SerialName("display_order")
    val displayOrder: Int? = null,
)
