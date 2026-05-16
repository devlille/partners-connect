package fr.devlille.partners.connect.ecosystempartners.domain

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EcosystemPartnerItem(
    val id: String,
    @SerialName("company_name")
    val companyName: String,
    @SerialName("company_logo_url")
    val companyLogoUrl: String? = null,
    val category: EcosystemPartnerCategory,
    @SerialName("display_order")
    val displayOrder: Int? = null,
    @SerialName("validated_at")
    val validatedAt: LocalDateTime? = null,
    @SerialName("declined_at")
    val declinedAt: LocalDateTime? = null,
    @SerialName("created_at")
    val createdAt: LocalDateTime,
)
