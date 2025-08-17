package fr.devlille.partners.connect.events.application

import fr.devlille.partners.connect.events.domain.Contact
import fr.devlille.partners.connect.events.domain.Event
import fr.devlille.partners.connect.events.domain.EventRepository
import fr.devlille.partners.connect.events.domain.EventSummary
import fr.devlille.partners.connect.events.domain.EventWithOrganisation
import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.events.infrastructure.db.EventsTable
import fr.devlille.partners.connect.internal.infrastructure.api.UnauthorizedException
import fr.devlille.partners.connect.internal.infrastructure.slugify.slugify
import fr.devlille.partners.connect.organisations.application.mappers.toDomain
import fr.devlille.partners.connect.organisations.infrastructure.db.OrganisationEntity
import fr.devlille.partners.connect.users.infrastructure.db.OrganisationPermissionEntity
import fr.devlille.partners.connect.users.infrastructure.db.OrganisationPermissionsTable
import fr.devlille.partners.connect.users.infrastructure.db.UserEntity
import fr.devlille.partners.connect.users.infrastructure.db.singleUserByEmail
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import fr.devlille.partners.connect.events.infrastructure.db.findBySlug as eventFindBySlug
import fr.devlille.partners.connect.organisations.infrastructure.db.findBySlug as orgFindBySlug

class EventRepositoryExposed(
    private val entity: UUIDEntityClass<EventEntity>,
) : EventRepository {
    override fun getAllEvents(): List<EventSummary> = transaction {
        entity.all().map {
            EventSummary(
                slug = it.slug,
                name = it.name,
                startTime = it.startTime,
                endTime = it.endTime,
                submissionStartTime = it.submissionStartTime,
                submissionEndTime = it.submissionEndTime,
            )
        }
    }

    override fun findByOrgSlug(orgSlug: String): List<EventSummary> = transaction {
        val organisation = OrganisationEntity.orgFindBySlug(orgSlug)
            ?: throw NotFoundException("Organisation with slug $orgSlug not found")
        entity.find { EventsTable.organisationId eq organisation.id }.map {
            EventSummary(
                slug = it.slug,
                name = it.name,
                startTime = it.startTime,
                endTime = it.endTime,
                submissionStartTime = it.submissionStartTime,
                submissionEndTime = it.submissionEndTime,
            )
        }
    }

    override fun getBySlug(eventSlug: String): EventWithOrganisation = transaction {
        val eventEntity = entity.eventFindBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")

        val event = Event(
            name = eventEntity.name,
            startTime = eventEntity.startTime,
            endTime = eventEntity.endTime,
            submissionStartTime = eventEntity.submissionStartTime,
            submissionEndTime = eventEntity.submissionEndTime,
            address = eventEntity.address,
            contact = Contact(phone = eventEntity.contactPhone, email = eventEntity.contactEmail),
        )

        val organisation = eventEntity.organisation.toDomain()

        EventWithOrganisation(
            event = event,
            organisation = organisation,
        )
    }

    override fun createEvent(orgSlug: String, event: Event): String = transaction {
        val organisation = OrganisationEntity.orgFindBySlug(orgSlug)
            ?: throw NotFoundException("Organisation with slug $orgSlug not found")

        val slug = event.name.slugify()

        entity.new {
            this.name = event.name
            this.slug = slug
            this.startTime = event.startTime
            this.endTime = event.endTime
            this.submissionStartTime = event.submissionStartTime
            this.submissionEndTime = event.submissionEndTime
            this.address = event.address
            this.contactPhone = event.contact.phone
            this.contactEmail = event.contact.email
            this.organisation = organisation
        }
        slug
    }

    override fun updateEvent(eventSlug: String, orgSlug: String, event: Event): String = transaction {
        val eventEntity = entity.eventFindBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")

        val newSlug = event.name.slugify()

        eventEntity.name = event.name
        eventEntity.slug = newSlug
        eventEntity.startTime = event.startTime
        eventEntity.endTime = event.endTime
        eventEntity.submissionStartTime = event.submissionStartTime
        eventEntity.submissionEndTime = event.submissionEndTime
        eventEntity.address = event.address
        eventEntity.contactPhone = event.contact.phone
        eventEntity.contactEmail = event.contact.email
        newSlug
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
                        slug = event.slug,
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
