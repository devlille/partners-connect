package fr.devlille.partners.connect.partnership.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RegisterPartnership(
    @SerialName("company_id")
    val companyId: String,
    @SerialName("pack_id")
    val packId: String,
    @SerialName("option_ids")
    val optionIds: List<String> = emptyList(),
    @SerialName("contact_name")
    val contactName: String,
    @SerialName("contact_role")
    val contactRole: String,
    val language: String,
    val phone: String? = null,
    val emails: List<String> = emptyList(),
)
