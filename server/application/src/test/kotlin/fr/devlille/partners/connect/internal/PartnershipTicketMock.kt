package fr.devlille.partners.connect.internal

import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipTicketEntity
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

@Suppress("LongParameterList")
fun insertMockPartnershipTicket(
    ticketId: UUID = UUID.randomUUID(),
    partnership: PartnershipEntity = insertMockPartnership(
        validatedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC),
    ),
    orderId: String = UUID.randomUUID().toString(),
    externalId: String = "ext-${UUID.randomUUID()}",
    firstname: String = "John",
    lastname: String = "Doe",
    email: String = "john.doe@mail.com",
    url: String = "https://example.com/ticket",
): PartnershipTicketEntity = transaction {
    PartnershipTicketEntity.new(ticketId.toString()) {
        this.partnership = partnership
        this.orderId = orderId
        this.externalId = externalId
        this.firstname = firstname
        this.lastname = lastname
        this.email = email
        this.url = url
    }
}
