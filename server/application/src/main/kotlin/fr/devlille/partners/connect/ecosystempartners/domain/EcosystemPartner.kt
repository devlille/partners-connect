package fr.devlille.partners.connect.ecosystempartners.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EcosystemPartner(
    val id: String,
    @SerialName("event_id")
    val eventId: String,
    @SerialName("company_id")
    val companyId: String,
    val category: EcosystemPartnerCategory,
    @SerialName("display_order")
    val displayOrder: Int? = null,
    val language: String,
    val emails: List<String> = emptyList(),
    @SerialName("validated_at")
    val validatedAt: String? = null,
    @SerialName("declined_at")
    val declinedAt: String? = null,
    @SerialName("created_at")
    val createdAt: String,
)
