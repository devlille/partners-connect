package fr.devlille.partners.connect.tickets.infrastructure.gateways.mappers

import fr.devlille.partners.connect.integrations.infrastructure.db.BilletWebConfig
import fr.devlille.partners.connect.partnership.infrastructure.db.BillingEntity
import fr.devlille.partners.connect.tickets.domain.TicketData
import fr.devlille.partners.connect.tickets.infrastructure.providers.models.CreateOrderProduct
import fr.devlille.partners.connect.tickets.infrastructure.providers.models.CreateOrderRequest

fun BillingEntity.toOrderRequest(
    tickets: List<TicketData>,
    config: BilletWebConfig,
): CreateOrderRequest = CreateOrderRequest(
    name = this.contactLastName,
    firstname = this.contactFirstName,
    email = this.contactEmail,
    products = tickets.map { ticket ->
        CreateOrderProduct(
            ticket = config.rateId,
            name = ticket.lastName,
            firstname = ticket.firstName,
            email = this.contactEmail,
        )
    },
)
