package fr.devlille.partners.connect.partnership.domain

import fr.devlille.partners.connect.events.domain.EventWithOrganisation
import fr.devlille.partners.connect.internal.infrastructure.system.SystemVarEnv
import fr.devlille.partners.connect.users.domain.User
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Partnership(
    val id: String,
    val phone: String? = null,
    val emails: List<String> = emptyList(),
    val language: String,
    @SerialName("selected_pack")
    val selectedPack: PartnershipPack? = null,
    @SerialName("suggestion_pack")
    val suggestionPack: PartnershipPack? = null,
    val organiser: User? = null,
)

@Serializable
data class PartnershipPack(
    val id: String,
    val name: String,
    @SerialName("base_price")
    val basePrice: Int,
    @SerialName("pack_price_override")
    val packPriceOverride: Int? = null,
    @SerialName("required_options")
    val requiredOptions: List<PartnershipOption>,
    @SerialName("optional_options")
    val optionalOptions: List<PartnershipOption>,
    @SerialName("total_price")
    val totalPrice: Int,
)

fun Partnership.link(
    event: EventWithOrganisation,
) = "${SystemVarEnv.frontendBaseUrl}/${event.event.slug}/$id"
