package fr.devlille.partners.connect.partnership.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PartnershipDetail(
    val id: String,
    val phone: String? = null,
    @SerialName("contact_name")
    val contactName: String,
    @SerialName("contact_role")
    val contactRole: String,
    val language: String,
    val emails: List<String> = emptyList(),
    @SerialName("selected_pack")
    val selectedPack: PartnershipPack? = null,
    @SerialName("suggestion_pack")
    val suggestionPack: PartnershipPack? = null,
    @SerialName("validated_pack")
    val validatedPack: PartnershipPack? = null,
    @SerialName("process_status")
    val processStatus: PartnershipProcessStatus,
    @SerialName("created_at")
    val createdAt: String,
)
