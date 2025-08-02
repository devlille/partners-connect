package fr.devlille.partners.connect.partnership.domain

import fr.devlille.partners.connect.sponsoring.domain.SponsoringOption
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class Partnership(
    val id: String,
    val phone: String? = null,
    val emails: List<String> = emptyList(),
    val language: String,
    @SerialName("selected_pack")
    val selectedPack: PartnershipPack? = null,
    @SerialName("suggestion_pack")
    val suggestionPack: PartnershipPack? = null,
)

@Serializable
class PartnershipPack(
    val id: String,
    val name: String,
    @SerialName("base_price")
    val basePrice: Int,
    val options: List<SponsoringOption>,
)
