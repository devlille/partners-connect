package fr.devlille.partners.connect.internal

import fr.devlille.partners.connect.partnership.infrastructure.db.BillingEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.InvoiceStatus
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEntity
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

@Suppress("LongParameterList")
fun insertMockedBilling(
    name: String = "Default Billing",
    firstName: String = "John",
    lastName: String = "Doe",
    email: String = "john@doe.com",
    po: String? = null,
    partnership: PartnershipEntity = insertMockPartnership(),
    status: InvoiceStatus = InvoiceStatus.PAID,
): BillingEntity = transaction {
    BillingEntity.new {
        this.event = partnership.event
        this.partnership = partnership
        this.name = name
        this.contactFirstName = firstName
        this.contactLastName = lastName
        this.contactEmail = email
        this.po = po
        this.status = status
    }
}
