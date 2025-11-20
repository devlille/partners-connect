package fr.devlille.partners.connect.tickets.infrastructure.gateways.mappers

import fr.devlille.partners.connect.tickets.domain.Ticket
import fr.devlille.partners.connect.tickets.domain.TicketData
import fr.devlille.partners.connect.tickets.domain.TicketOrder
import fr.devlille.partners.connect.tickets.infrastructure.providers.models.CreateOrderResponseItem
import fr.devlille.partners.connect.tickets.infrastructure.providers.models.ProductDetail

fun CreateOrderResponseItem.toDomain(data: List<TicketData>): TicketOrder {
    require(data.size == productsDetails.size) { "Data size must match products details size" }
    return TicketOrder(
        id = id,
        tickets = productsDetails.mapIndexed { index, productDetail ->
            productDetail.toDomain(data[index])
        },
    )
}

fun ProductDetail.toDomain(data: TicketData): Ticket = Ticket(
    id = id,
    extId = extId,
    url = productDownload,
    data = data,
)
