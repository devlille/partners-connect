package fr.devlille.partners.connect.partnership.infrastructure.db

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.Entity
import org.jetbrains.exposed.v1.dao.EntityClass
import org.jetbrains.exposed.v1.jdbc.SizedIterable
import java.util.UUID

class PartnershipTicketEntity(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, PartnershipTicketEntity>(PartnershipTicketsTable) {
        fun listByPartnership(partnershipId: UUID): SizedIterable<PartnershipTicketEntity> = this
            .find { PartnershipTicketsTable.partnershipId eq partnershipId }
    }

    var partnership by PartnershipEntity referencedOn PartnershipTicketsTable.partnershipId
    var orderId by PartnershipTicketsTable.orderId
    var externalId by PartnershipTicketsTable.externalId
    var url by PartnershipTicketsTable.url
    var firstname by PartnershipTicketsTable.firstname
    var lastname by PartnershipTicketsTable.lastname
    var email by PartnershipTicketsTable.email
}
