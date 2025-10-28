package fr.devlille.partners.connect.sponsoring.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class CreateSponsoringPack(
    val name: String,
    val price: Int,
    @SerialName("booth_size")
    val boothSize: String? = null,
    @SerialName("nb_tickets")
    val nbTickets: Int,
    @SerialName("max_quantity")
    val maxQuantity: Int? = null,
)
