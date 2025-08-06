package fr.devlille.partners.connect.partnership.infrastructure.db

import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import java.util.UUID

class InvoiceEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<InvoiceEntity>(InvoicesTable)

    var event by EventEntity referencedOn InvoicesTable.eventId
    var partnership by PartnershipEntity referencedOn InvoicesTable.partnershipId
    var name by InvoicesTable.name
    var contactFirstName by InvoicesTable.contactFirstName
    var contactLastName by InvoicesTable.contactLastName
    var contactEmail by InvoicesTable.contactEmail
    var address by InvoicesTable.address
    var city by InvoicesTable.city
    var zipCode by InvoicesTable.zipCode
    var country by InvoicesTable.country
    var siret by InvoicesTable.siret
    var vat by InvoicesTable.vat
    var po by InvoicesTable.po
    var invoicePdfUrl by InvoicesTable.invoicePdfUrl
    var status by InvoicesTable.status
    var createdAt by InvoicesTable.createdAt
}

fun UUIDEntityClass<InvoiceEntity>.singleByEventAndPartnership(
    eventId: UUID,
    partnershipId: UUID,
): InvoiceEntity? = this
    .find { (InvoicesTable.eventId eq eventId) and (InvoicesTable.partnershipId eq partnershipId) }
    .singleOrNull()
