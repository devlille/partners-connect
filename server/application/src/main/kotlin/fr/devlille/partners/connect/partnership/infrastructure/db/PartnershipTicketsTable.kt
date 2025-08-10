package fr.devlille.partners.connect.partnership.infrastructure.db

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.dao.id.IdTable

object PartnershipTicketsTable : IdTable<String>("partnership_tickets") {
    override val id: Column<EntityID<String>> = varchar("ticket_id", length = 50).entityId()
    val partnershipId = reference("partnership_id", PartnershipsTable)
    val orderId = varchar("order_id", length = 50)
    val externalId = varchar("external_id", length = 50)
    val url = varchar("download_url", length = 512)
    val firstname = varchar("firstname", length = 100)
    val lastname = varchar("lastname", length = 100)
    val email = varchar("email", length = 255)
}
