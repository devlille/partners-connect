package fr.devlille.partners.connect.partnership.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PartnershipItem(
    val id: String,
    val contact: ContactInfo,
    @SerialName("company_name")
    val companyName: String,
    @SerialName("pack_name")
    val packName: String,
    @SerialName("suggested_pack_name")
    val suggestedPackName: String? = null,
    val language: String,
    val phone: String? = null,
    val emails: List<String> = emptyList(),
    @SerialName("created_at")
    val createdAt: String,
)

@Serializable
data class ContactInfo(
    @SerialName("display_name")
    val displayName: String,
    val role: String,
)

@Serializable
data class PartnershipFilters(
    val packId: String? = null,
    val validated: Boolean? = null,
    val suggestion: Boolean? = null,
    val paid: Boolean? = null,
    val agreementGenerated: Boolean? = null,
    val agreementSigned: Boolean? = null,
)
