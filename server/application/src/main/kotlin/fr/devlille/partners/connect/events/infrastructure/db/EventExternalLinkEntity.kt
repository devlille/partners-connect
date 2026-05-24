package fr.devlille.partners.connect.events.infrastructure.db

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.java.UUIDEntity
import org.jetbrains.exposed.v1.dao.java.UUIDEntityClass
import java.util.UUID

class EventExternalLinkEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<EventExternalLinkEntity>(EventExternalLinksTable)

    var event by EventEntity referencedOn EventExternalLinksTable.eventId
    var name by EventExternalLinksTable.name
    var url by EventExternalLinksTable.url
}
