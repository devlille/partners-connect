package fr.devlille.partners.connect.tickets.infrastructure.gateways.mappers

import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipTicketEntity
import fr.devlille.partners.connect.tickets.domain.Ticket
import fr.devlille.partners.connect.tickets.domain.TicketData

fun PartnershipTicketEntity.toDomain(): Ticket = Ticket(
    id = id.value,
    extId = this.externalId,
    url = this.url,
    data = TicketData(
        firstName = this.firstname,
        lastName = this.lastname,
    ),
)
