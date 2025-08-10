package fr.devlille.partners.connect.tickets.infrastructure.gateways.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateOrderRequest(
    val name: String,
    val firstname: String,
    val email: String,
    @SerialName("payment_type")
    val paymentType: String = "free",
    val products: List<CreateOrderProduct>,
)

@Serializable
data class CreateOrderProduct(
    val ticket: String,
    val name: String,
    val firstname: String,
    val email: String,
    val used: String = "0",
)
