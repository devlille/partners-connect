package fr.devlille.partners.connect.tickets.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class TicketData(
    @SerialName("first_name")
    val firstName: String,
    @SerialName("last_name")
    val lastName: String,
)
