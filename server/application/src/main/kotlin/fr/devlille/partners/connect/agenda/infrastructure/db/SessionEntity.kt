package fr.devlille.partners.connect.agenda.infrastructure.db

import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import java.util.UUID

class SessionEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<SessionEntity>(SessionsTable)

    var externalId by SessionsTable.externalId
    var name by SessionsTable.name
    var abstract by SessionsTable.abstract
    var startTime by SessionsTable.startTime
    var endTime by SessionsTable.endTime
    var trackName by SessionsTable.trackName
    var language by SessionsTable.language
    var createdAt by SessionsTable.createdAt
    var event by EventEntity referencedOn SessionsTable.eventId
}
