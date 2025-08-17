package fr.devlille.partners.connect.events.infrastructure.db

import fr.devlille.partners.connect.organisations.infrastructure.db.OrganisationEntity
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import java.util.UUID

class EventEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<EventEntity>(EventsTable)

    var name by EventsTable.name
    var slug by EventsTable.slug
    var startTime by EventsTable.startTime
    var endTime by EventsTable.endTime
    var submissionStartTime by EventsTable.submissionStartTime
    var submissionEndTime by EventsTable.submissionEndTime
    var address by EventsTable.address
    var contactPhone by EventsTable.contactPhone
    var contactEmail by EventsTable.contactEmail
    var organisation by OrganisationEntity referencedOn EventsTable.organisationId
}

fun UUIDEntityClass<EventEntity>.findBySlug(slug: String): EventEntity? = 
    this.find { EventsTable.slug eq slug }.singleOrNull()
