package fr.devlille.partners.connect.events.application

import fr.devlille.partners.connect.events.domain.Contact
import fr.devlille.partners.connect.events.domain.CreateEventExternalLinkRequest
import fr.devlille.partners.connect.events.domain.Event
import fr.devlille.partners.connect.events.domain.EventDisplay
import fr.devlille.partners.connect.events.domain.EventExternalLink
import fr.devlille.partners.connect.events.domain.EventRepository
import fr.devlille.partners.connect.events.domain.EventSummary
import fr.devlille.partners.connect.events.domain.EventWithOrganisation
import fr.devlille.partners.connect.events.domain.PaginatedResponse
import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.events.infrastructure.db.EventExternalLinkEntity
import fr.devlille.partners.connect.events.infrastructure.db.EventExternalLinksTable
import fr.devlille.partners.connect.events.infrastructure.db.EventsTable
import fr.devlille.partners.connect.internal.infrastructure.api.UnauthorizedException
import fr.devlille.partners.connect.internal.infrastructure.slugify.slugify
import fr.devlille.partners.connect.organisations.application.mappers.toItemDomain
import fr.devlille.partners.connect.organisations.infrastructure.db.OrganisationEntity
import fr.devlille.partners.connect.provider.domain.Provider
import fr.devlille.partners.connect.provider.infrastructure.db.EventProviderEntity
import fr.devlille.partners.connect.provider.infrastructure.db.EventProvidersTable
import fr.devlille.partners.connect.provider.infrastructure.db.ProviderEntity
import fr.devlille.partners.connect.users.infrastructure.db.OrganisationPermissionEntity
import fr.devlille.partners.connect.users.infrastructure.db.OrganisationPermissionsTable
import fr.devlille.partners.connect.users.infrastructure.db.UserEntity
import fr.devlille.partners.connect.users.infrastructure.db.singleUserByEmail
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import org.jetbrains.exposed.v1.jdbc.selectAll
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

    override fun findByOrgSlugPaginated(
        orgSlug: String,
        page: Int,
        pageSize: Int
    ): PaginatedResponse<EventSummary> = transaction {
        if (page < 1) {
            throw BadRequestException("Page number must be greater than 0")
        }
        if (pageSize < 1) {
            throw BadRequestException("Page size must be greater than 0")
        }

        val organisation = OrganisationEntity.orgFindBySlug(orgSlug)
            ?: throw NotFoundException("Organisation with slug $orgSlug not found")
        val orgId = organisation.id
        val eventQuery = EventsTable.selectAll().where {
            EventsTable.organisationId eq orgId
        }
        val total = eventQuery.count()
        val offset = (page - 1) * pageSize
        val rows = eventQuery
            .orderBy(EventsTable.startTime)
            .limit(pageSize)
            .offset(offset.toLong())
            .toList()
        val items = rows.map { row ->
            val eventEntity = EventEntity[row[EventsTable.id]]
            EventSummary(
                slug = eventEntity.slug,
                name = eventEntity.name,
                startTime = eventEntity.startTime,
                endTime = eventEntity.endTime,
                submissionStartTime = eventEntity.submissionStartTime,
                submissionEndTime = eventEntity.submissionEndTime
            )
        }
        PaginatedResponse(
            items = items,
            page = page,
            pageSize = pageSize,
            total = total
        )
    }

    override fun getBySlug(eventSlug: String): EventWithOrganisation = transaction {
        val eventEntity = entity.eventFindBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")

        // Fetch external links directly in the same transaction
        val externalLinks = EventExternalLinkEntity.find {
            EventExternalLinksTable.eventId eq eventEntity.id
        }.map { linkEntity ->
            EventExternalLink(
                id = linkEntity.id.value.toString(),
                name = linkEntity.name,
                url = linkEntity.url,
            )
        }

        // Fetch providers attached to this event
        val providers = EventProviderEntity.find {
            EventProvidersTable.eventId eq eventEntity.id
        }.map { eventProvider ->
            val provider = ProviderEntity.findById(eventProvider.providerId)!!
            Provider(
                id = provider.id.value.toString(),
                name = provider.name,
                type = provider.type,
                website = provider.website,
                phone = provider.phone,
                email = provider.email,
                createdAt = provider.createdAt,
            )
        }

        val event = EventDisplay(
            slug = eventEntity.slug,
            name = eventEntity.name,
            startTime = eventEntity.startTime,
            endTime = eventEntity.endTime,
            submissionStartTime = eventEntity.submissionStartTime,
            submissionEndTime = eventEntity.submissionEndTime,
            address = eventEntity.address,
            contact = Contact(phone = eventEntity.contactPhone, email = eventEntity.contactEmail),
            externalLinks = externalLinks,
            providers = providers,
        )

        val organisation = eventEntity.organisation.toItemDomain()

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

        eventEntity.name = event.name
        eventEntity.startTime = event.startTime
        eventEntity.endTime = event.endTime
        eventEntity.submissionStartTime = event.submissionStartTime
        eventEntity.submissionEndTime = event.submissionEndTime
        eventEntity.address = event.address
        eventEntity.contactPhone = event.contact.phone
        eventEntity.contactEmail = event.contact.email
        eventSlug
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

    override fun updateBoothPlanImageUrl(eventSlug: String, imageUrl: String): Unit = transaction {
        val eventEntity = entity.eventFindBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")

        eventEntity.boothPlanImageUrl = imageUrl
    }

    override fun createExternalLink(
        eventSlug: String,
        request: CreateEventExternalLinkRequest,
    ): UUID = transaction {
        // Basic validation
        if (request.name.isBlank()) {
            throw BadRequestException("External link name cannot be empty")
        }
        if (request.url.isBlank()) {
            throw BadRequestException("External link URL cannot be empty")
        }

        // Basic URL validation
        val urlPattern = Regex("^https?://.*")
        if (!urlPattern.matches(request.url)) {
            throw BadRequestException("Invalid URL format - must start with http:// or https://")
        }

        val eventEntity = entity.eventFindBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")

        val externalLinkEntity = EventExternalLinkEntity.new {
            this.event = eventEntity
            this.name = request.name
            this.url = request.url
        }

        externalLinkEntity.id.value
    }

    override fun deleteExternalLink(externalLinkId: UUID): Unit = transaction {
        val linkEntity = EventExternalLinkEntity.findById(externalLinkId)
            ?: throw NotFoundException("External link with id $externalLinkId not found")

        linkEntity.delete()
    }
}
