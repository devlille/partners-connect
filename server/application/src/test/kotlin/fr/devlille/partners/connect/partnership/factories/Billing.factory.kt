package fr.devlille.partners.connect.partnership.factories

import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.partnership.domain.InvoiceStatus
import fr.devlille.partners.connect.partnership.infrastructure.db.BillingEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEntity
import java.util.UUID

@Suppress("LongParameterList")
fun insertMockedBilling(
    eventId: UUID = UUID.randomUUID(),
    partnershipId: UUID = UUID.randomUUID(),
    name: String = partnershipId.toString(),
    firstName: String = "John",
    lastName: String = "Doe",
    email: String = "john@doe.com",
    po: String? = null,
    status: InvoiceStatus = InvoiceStatus.PAID,
): BillingEntity = BillingEntity.new {
    this.event = EventEntity[eventId]
    this.partnership = PartnershipEntity[partnershipId]
    this.name = name
    this.contactFirstName = firstName
    this.contactLastName = lastName
    this.contactEmail = email
    this.po = po
    this.status = status
}
