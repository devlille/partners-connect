package fr.devlille.partners.connect.events.infrastructure.db

import fr.devlille.partners.connect.legaentity.infrastructure.db.LegalEntityEntity
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import java.util.UUID

class EventEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<EventEntity>(EventsTable)

    var name by EventsTable.name
    var startTime by EventsTable.startTime
    var endTime by EventsTable.endTime
    var submissionStartTime by EventsTable.submissionStartTime
    var submissionEndTime by EventsTable.submissionEndTime
    var address by EventsTable.address
    var contactPhone by EventsTable.contactPhone
    var contactEmail by EventsTable.contactEmail
    var legalEntity by LegalEntityEntity referencedOn EventsTable.legalEntityId
}
