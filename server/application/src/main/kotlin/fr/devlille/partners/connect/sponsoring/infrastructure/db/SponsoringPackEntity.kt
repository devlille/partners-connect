package fr.devlille.partners.connect.sponsoring.infrastructure.db

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import java.util.UUID

class SponsoringPackEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<SponsoringPackEntity>(SponsoringPacksTable)

    var eventId by SponsoringPacksTable.eventId
    var name by SponsoringPacksTable.name
    var basePrice by SponsoringPacksTable.basePrice
    var maxQuantity by SponsoringPacksTable.maxQuantity
    val options by SponsoringOptionEntity via PackOptionsTable
}
