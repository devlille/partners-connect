package fr.devlille.partners.connect.ecosystempartners.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UpdateEcosystemPartner(
    @SerialName("category_id")
    val categoryId: String? = null,
    @SerialName("display_order")
    val displayOrder: Int? = null,
    val language: String? = null,
    val emails: List<String>? = null,
)
