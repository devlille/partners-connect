package fr.devlille.partners.connect.partnership.factories

import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipTicketEntity
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

@Suppress("LongParameterList")
fun insertMockedPartnershipTicket(
    ticketId: UUID = UUID.randomUUID(),
    partnershipId: UUID = UUID.randomUUID(),
    orderId: String = UUID.randomUUID().toString(),
    externalId: String = "ext-${UUID.randomUUID()}",
    firstname: String = "John",
    lastname: String = "Doe",
    email: String = "john.doe@mail.com",
    url: String = "https://example.com/ticket",
): PartnershipTicketEntity = transaction {
    PartnershipTicketEntity.new(ticketId.toString()) {
        this.partnership = PartnershipEntity[partnershipId]
        this.orderId = orderId
        this.externalId = externalId
        this.firstname = firstname
        this.lastname = lastname
        this.email = email
        this.url = url
    }
}
