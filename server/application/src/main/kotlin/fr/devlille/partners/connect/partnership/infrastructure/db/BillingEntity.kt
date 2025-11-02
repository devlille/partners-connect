package fr.devlille.partners.connect.partnership.infrastructure.db

import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import java.util.UUID

class BillingEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<BillingEntity>(BillingsTable) {
        fun singleByEventAndPartnership(eventId: UUID, partnershipId: UUID): BillingEntity? = this
            .find { (BillingsTable.eventId eq eventId) and (BillingsTable.partnershipId eq partnershipId) }
            .singleOrNull()
    }

    var event by EventEntity referencedOn BillingsTable.eventId
    var partnership by PartnershipEntity referencedOn BillingsTable.partnershipId
    var name by BillingsTable.name
    var contactFirstName by BillingsTable.contactFirstName
    var contactLastName by BillingsTable.contactLastName
    var contactEmail by BillingsTable.contactEmail
    var po by BillingsTable.po
    var invoicePdfUrl by BillingsTable.invoicePdfUrl
    var quotePdfUrl by BillingsTable.quotePdfUrl
    var status by BillingsTable.status
    var createdAt by BillingsTable.createdAt
}
