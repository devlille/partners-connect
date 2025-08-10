package fr.devlille.partners.connect.tickets.infrastructure.gateways.models.mappers

import fr.devlille.partners.connect.integrations.infrastructure.db.BilletWebConfig
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipTicketEntity
import fr.devlille.partners.connect.tickets.infrastructure.gateways.models.CreateOrderProduct

fun PartnershipTicketEntity.toCreateOrderProduct(config: BilletWebConfig): CreateOrderProduct = CreateOrderProduct(
    name = this.lastname,
    firstname = this.firstname,
    ticket = config.rateId,
    email = this.email,
)
