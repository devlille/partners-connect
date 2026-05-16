package fr.devlille.partners.connect.ecosystempartners.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RegisterEcosystemPartnerCategory(
    val name: String,
    @SerialName("display_order")
    val displayOrder: Int? = null,
)
