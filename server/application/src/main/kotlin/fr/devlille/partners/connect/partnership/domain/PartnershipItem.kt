package fr.devlille.partners.connect.partnership.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PartnershipItem(
    val id: String,
    val contact: Contact,
    @SerialName("company_name")
    val companyName: String,
    @SerialName("event_name")
    val eventName: String,
    @SerialName("pack_name")
    val packName: String? = null,
    @SerialName("suggested_pack_name")
    val suggestedPackName: String? = null,
    val language: String,
    val phone: String? = null,
    val emails: List<String> = emptyList(),
    @SerialName("created_at")
    val createdAt: String,
)

@Serializable
data class Contact(
    @SerialName("display_name")
    val displayName: String,
    val role: String,
)
