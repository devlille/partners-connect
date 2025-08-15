package fr.devlille.partners.connect.events.application

import fr.devlille.partners.connect.events.domain.Contact
import fr.devlille.partners.connect.events.domain.Event
import fr.devlille.partners.connect.events.domain.EventRepository
import fr.devlille.partners.connect.events.domain.EventSummary
import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.internal.infrastructure.uuid.toUUID
import fr.devlille.partners.connect.organisations.infrastructure.db.OrganisationEntity
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

class EventRepositoryExposed(
    private val entity: UUIDEntityClass<EventEntity>,
) : EventRepository {
    override fun getAllEvents(): List<EventSummary> = transaction {
        entity.all().map {
            EventSummary(
                id = it.id.value.toString(),
                name = it.name,
                startTime = it.startTime,
                endTime = it.endTime,
                submissionStartTime = it.submissionStartTime,
                submissionEndTime = it.submissionEndTime,
            )
        }
    }

    override fun getById(eventId: UUID): Event = transaction {
        val event = entity.findById(eventId)
            ?: throw NotFoundException("Event with id $eventId not found")
        Event(
            name = event.name,
            startTime = event.startTime,
            endTime = event.endTime,
            submissionStartTime = event.submissionStartTime,
            submissionEndTime = event.submissionEndTime,
            address = event.address,
            contact = Contact(phone = event.contactPhone, email = event.contactEmail),
            organisationId = event.organisation.id.value.toString(),
        )
    }

    override fun createEvent(event: Event): UUID = transaction {
        val organisation = OrganisationEntity.findById(event.organisationId.toUUID())
            ?: throw NotFoundException("Organisation with id ${event.organisationId} not found")
        entity.new {
            this.name = event.name
            this.startTime = event.startTime
            this.endTime = event.endTime
            this.submissionStartTime = event.submissionStartTime
            this.submissionEndTime = event.submissionEndTime
            this.address = event.address
            this.contactPhone = event.contact.phone
            this.contactEmail = event.contact.email
            this.organisation = organisation
        }.id.value
    }

    override fun updateEvent(id: UUID, event: Event): UUID = transaction {
        val organisation = OrganisationEntity.findById(event.organisationId.toUUID())
            ?: throw NotFoundException("Organisation with id ${event.organisationId} not found")
        val entity = entity.findById(id) ?: throw IllegalArgumentException("Event not found")
        entity.name = event.name
        entity.startTime = event.startTime
        entity.endTime = event.endTime
        entity.submissionStartTime = event.submissionStartTime
        entity.submissionEndTime = event.submissionEndTime
        entity.address = event.address
        entity.contactPhone = event.contact.phone
        entity.contactEmail = event.contact.email
        entity.organisation = organisation
        entity.id.value
    }
}
