package fr.devlille.partners.connect.partnership.domain

import fr.devlille.partners.connect.users.domain.User
import kotlinx.datetime.LocalDateTime
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
    @SerialName("selected_pack_id")
    val selectedPackId: String? = null,
    @SerialName("selected_pack_name")
    val selectedPackName: String? = null,
    @SerialName("suggested_pack_id")
    val suggestedPackId: String? = null,
    @SerialName("suggested_pack_name")
    val suggestedPackName: String? = null,
    @SerialName("validated_pack_id")
    val validatedPackId: String? = null,
    val language: String,
    val phone: String? = null,
    val emails: List<String> = emptyList(),
    val organiser: User? = null,
    @SerialName("created_at")
    val createdAt: LocalDateTime,
)

@Serializable
data class Contact(
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
    val organiser: String? = null,
)
