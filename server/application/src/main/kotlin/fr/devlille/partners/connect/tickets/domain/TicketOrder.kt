package fr.devlille.partners.connect.tickets.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TicketOrder(
    val id: String,
    val tickets: List<Ticket>,
)

@Serializable
data class Ticket(
    val id: String,
    @SerialName("external_id")
    val extId: String,
    val url: String,
    val data: TicketData,
)
