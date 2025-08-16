package fr.devlille.partners.connect.events.application

import fr.devlille.partners.connect.events.domain.Contact
import fr.devlille.partners.connect.events.domain.Event
import fr.devlille.partners.connect.events.domain.EventRepository
import fr.devlille.partners.connect.events.domain.EventSummary
import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.events.infrastructure.db.EventsTable
import fr.devlille.partners.connect.internal.infrastructure.api.UnauthorizedException
import fr.devlille.partners.connect.organisations.infrastructure.db.OrganisationEntity
import fr.devlille.partners.connect.organisations.infrastructure.db.findBySlug
import fr.devlille.partners.connect.users.infrastructure.db.OrganisationPermissionEntity
import fr.devlille.partners.connect.users.infrastructure.db.OrganisationPermissionsTable
import fr.devlille.partners.connect.users.infrastructure.db.UserEntity
import fr.devlille.partners.connect.users.infrastructure.db.singleUserByEmail
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.core.and
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

    override fun findByOrgSlug(orgSlug: String): List<EventSummary> = transaction {
        val organisation = OrganisationEntity.findBySlug(orgSlug)
            ?: throw NotFoundException("Organisation with slug $orgSlug not found")
        entity.find { EventsTable.organisationId eq organisation.id }.map {
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
        )
    }

    override fun createEvent(orgSlug: String, event: Event): UUID = transaction {
        val organisation = OrganisationEntity.findBySlug(orgSlug)
            ?: throw NotFoundException("Organisation with slug $orgSlug not found")
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

    override fun updateEvent(id: UUID, orgSlug: String, event: Event): UUID = transaction {
        val entity = entity.findById(id) ?: throw IllegalArgumentException("Event not found")
        entity.name = event.name
        entity.startTime = event.startTime
        entity.endTime = event.endTime
        entity.submissionStartTime = event.submissionStartTime
        entity.submissionEndTime = event.submissionEndTime
        entity.address = event.address
        entity.contactPhone = event.contact.phone
        entity.contactEmail = event.contact.email
        entity.id.value
    }

    override fun findByUserEmail(userEmail: String): List<EventSummary> = transaction {
        val user = UserEntity.singleUserByEmail(userEmail)
            ?: throw NotFoundException("User with email $userEmail not found")

        // Find all organizations where the user has edit permissions
        val userPermissions = OrganisationPermissionEntity
            .find {
                (OrganisationPermissionsTable.userId eq user.id.value) and
                    (OrganisationPermissionsTable.canEdit eq true)
            }

        // Check if user has any organizer permissions
        if (userPermissions.empty()) {
            throw UnauthorizedException("You do not have organizer permissions")
        }

        // Get all events from those organizations
        val events = mutableListOf<EventSummary>()
        userPermissions.forEach { permission ->
            val orgEvents = entity.find {
                EventsTable.organisationId eq permission.organisation.id
            }
            orgEvents.forEach { event ->
                events.add(
                    EventSummary(
                        id = event.id.value.toString(),
                        name = event.name,
                        startTime = event.startTime,
                        endTime = event.endTime,
                        submissionStartTime = event.submissionStartTime,
                        submissionEndTime = event.submissionEndTime,
                    ),
                )
            }
        }
        events
    }
}
