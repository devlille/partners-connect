package fr.devlille.partners.connect.sponsoring.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class CreateSponsoringPack(
    val name: String,
    val price: Int,
    @SerialName("with_booth")
    val withBooth: Boolean = false,
    @SerialName("max_quantity")
    val maxQuantity: Int? = null,
)
