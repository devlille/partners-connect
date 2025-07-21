package fr.devlille.partners.connect.partnership.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class RegisterPartnership(
    @SerialName("pack_id")
    val packId: String,
    @SerialName("option_ids")
    val optionIds: List<String> = emptyList(),
    val language: String,
    val phone: String? = null,
    val emails: List<String> = emptyList(),
)
