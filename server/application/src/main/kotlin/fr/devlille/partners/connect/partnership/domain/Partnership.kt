package fr.devlille.partners.connect.partnership.domain

import fr.devlille.partners.connect.sponsoring.domain.SponsoringPack
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class Partnership(
    val id: String,
    val phone: String? = null,
    val emails: List<String> = emptyList(),
    val language: String,
    @SerialName("selected_pack")
    val selectedPack: SponsoringPack? = null,
    @SerialName("suggestion_pack")
    val suggestionPack: SponsoringPack? = null,
    @SerialName("suggestion_sent_at")
    val suggestionSentAt: String? = null,
    @SerialName("suggestion_approved_at")
    val suggestionApprovedAt: String? = null,
    @SerialName("suggestion_declined_at")
    val suggestionDeclinedAt: String? = null,
)
