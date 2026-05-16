package fr.devlille.partners.connect.ecosystempartners.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RegisterEcosystemPartner(
    @SerialName("company_id")
    val companyId: String,
    @SerialName("category_id")
    val categoryId: String,
    @SerialName("display_order")
    val displayOrder: Int? = null,
    val language: String = "en",
    val emails: List<String> = emptyList(),
)
