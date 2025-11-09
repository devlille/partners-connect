package fr.devlille.partners.connect.provider.application

import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.internal.infrastructure.api.EmptyListValidationException
import fr.devlille.partners.connect.internal.infrastructure.api.ForbiddenException
import fr.devlille.partners.connect.internal.infrastructure.api.PaginatedResponse
import fr.devlille.partners.connect.internal.infrastructure.api.paginated
import fr.devlille.partners.connect.internal.infrastructure.api.toPaginatedResponse
import fr.devlille.partners.connect.organisations.infrastructure.db.OrganisationEntity
import fr.devlille.partners.connect.provider.application.mappers.toDomain
import fr.devlille.partners.connect.provider.domain.CreateProvider
import fr.devlille.partners.connect.provider.domain.Provider
import fr.devlille.partners.connect.provider.domain.ProviderRepository
import fr.devlille.partners.connect.provider.domain.UpdateProvider
import fr.devlille.partners.connect.provider.infrastructure.db.EventProviderEntity
import fr.devlille.partners.connect.provider.infrastructure.db.EventProvidersTable
import fr.devlille.partners.connect.provider.infrastructure.db.ProviderEntity
import fr.devlille.partners.connect.provider.infrastructure.db.ProvidersTable
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import fr.devlille.partners.connect.events.infrastructure.db.findBySlug as eventFindBySlug
import fr.devlille.partners.connect.organisations.infrastructure.db.findBySlug as orgFindBySlug

/**
 * Exposed ORM implementation of the ProviderRepository interface.
 *
 * Provides organisation-scoped data access for provider entities using PostgreSQL database
 * with Exposed ORM. Implements all CRUD operations, event attachment management, and
 * public listing with proper organisation-level access control.
 *
 * Key features:
 * - Organisation scoping for all operations (except public listing)
 * - Text search across provider names and types
 * - Sorting by name, type, or creation date
 * - Pagination support for large result sets
 * - Event attachment/detachment with referential integrity
 * - Transactional consistency for all database operations
 */
class ProviderRepositoryExposed : ProviderRepository {
    override fun list(
        orgSlug: String?,
        query: String?,
        sort: String?,
        direction: String?,
        page: Int,
        pageSize: Int,
    ): PaginatedResponse<Provider> = transaction {
        var queryBuilder = if (orgSlug != null) {
            // Organization-scoped listing
            val organisation = OrganisationEntity.orgFindBySlug(orgSlug)
                ?: throw NotFoundException("Organisation not found")

            ProviderEntity.find {
                ProvidersTable.organisationId eq organisation.id
            }
        } else {
            // Public listing (all providers)
            ProviderEntity.all()
        }

        // Apply search filter if query is provided
        if (!query.isNullOrBlank()) {
            queryBuilder = if (orgSlug != null) {
                val organisation = OrganisationEntity.orgFindBySlug(orgSlug)
                    ?: throw NotFoundException("Organisation not found")

                ProviderEntity.find {
                    (ProvidersTable.organisationId eq organisation.id) and
                        (
                            (ProvidersTable.name.lowerCase() like "%${query.lowercase()}%") or
                                (ProvidersTable.type.lowerCase() like "%${query.lowercase()}%")
                        )
                }
            } else {
                ProviderEntity.find {
                    ProvidersTable.name.lowerCase() like "%${query.lowercase()}%"
                }
            }
        }

        // Apply sorting
        val sortOrder = when (direction?.lowercase()) {
            "desc" -> SortOrder.DESC
            else -> SortOrder.ASC // default to ASC
        }

        val sortColumn = when (sort?.lowercase()) {
            "name" -> ProvidersTable.name
            "type" -> ProvidersTable.type
            "creation", "created", "createdat", "created_at" -> ProvidersTable.createdAt
            else -> ProvidersTable.createdAt // default to createdAt
        }

        val total = queryBuilder.count()
        val paginated = queryBuilder.orderBy(sortColumn to sortOrder).paginated(page, pageSize)
        val items = paginated.map { entity -> entity.toDomain() }
        items.toPaginatedResponse(total, page, pageSize)
    }

    override fun findByIdAndOrganisation(providerId: UUID, orgSlug: String): Provider = transaction {
        val organisation = OrganisationEntity.orgFindBySlug(orgSlug)
            ?: throw NotFoundException("Organisation not found")

        ProviderEntity
            .find { (ProvidersTable.id eq providerId) and (ProvidersTable.organisationId eq organisation.id) }
            .firstOrNull()
            ?.toDomain()
            ?: throw NotFoundException("Provider not found")
    }

    override fun findByEvent(eventSlug: String, page: Int, pageSize: Int): PaginatedResponse<Provider> = transaction {
        val eventEntity = EventEntity.eventFindBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        val eventProviders = EventProviderEntity
            .find { EventProvidersTable.eventId eq eventEntity.id }
        val totalCount = eventProviders.count()
        val providers = eventProviders
            .paginated(page, pageSize)
            .map { it.provider.toDomain() }
        providers.toPaginatedResponse(totalCount, page, pageSize)
    }

    override fun attachToEvent(orgSlug: String, eventSlug: String, providerIds: List<UUID>): List<UUID> = transaction {
        if (providerIds.isEmpty()) {
            throw EmptyListValidationException("ids")
        }

        // First, find the organization
        val organisationEntity = OrganisationEntity.orgFindBySlug(orgSlug)
            ?: throw NotFoundException("Organisation with slug $orgSlug not found")

        val eventEntity = EventEntity.eventFindBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")

        // Validate that the event belongs to the organization
        if (eventEntity.organisation.id.value != organisationEntity.id.value) {
            throw ForbiddenException("Event $eventSlug does not belong to organization $orgSlug")
        }

        // Validate that all provider IDs exist and belong to the organization
        val providerEntities = ProviderEntity.find {
            (ProvidersTable.id inList providerIds) and (ProvidersTable.organisationId eq organisationEntity.id)
        }.toList()

        if (providerEntities.size != providerIds.size) {
            throw ForbiddenException("One or more provider IDs do not exist or do not belong to the organization")
        }

        val attachedIds = mutableListOf<UUID>()
        providerEntities.forEach { providerEntity ->
            // Check if the relationship already exists
            val existing = EventProviderEntity.find {
                (EventProvidersTable.eventId eq eventEntity.id) and
                    (EventProvidersTable.providerId eq providerEntity.id)
            }.firstOrNull()

            if (existing == null) {
                // Create the relationship
                EventProviderEntity.new {
                    event = eventEntity
                    provider = providerEntity
                }
            }
            attachedIds.add(providerEntity.id.value)
        }

        attachedIds
    }

    override fun detachFromEvent(
        orgSlug: String,
        eventSlug: String,
        providerIds: List<UUID>,
    ): List<UUID> = transaction {
        if (providerIds.isEmpty()) {
            throw EmptyListValidationException("ids")
        }

        // First, find the organization
        val organisationEntity = OrganisationEntity.orgFindBySlug(orgSlug)
            ?: throw NotFoundException("Organisation with slug $orgSlug not found")

        val eventEntity = EventEntity.eventFindBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")

        // Validate that the event belongs to the organization
        if (eventEntity.organisation.id.value != organisationEntity.id.value) {
            throw ForbiddenException("Event $eventSlug does not belong to organization $orgSlug")
        }

        // Validate that all provider IDs exist and belong to the organization
        val providerEntities = ProviderEntity.find {
            (ProvidersTable.id inList providerIds) and (ProvidersTable.organisationId eq organisationEntity.id)
        }.toList()

        if (providerEntities.size != providerIds.size) {
            throw ForbiddenException("One or more provider IDs do not exist or do not belong to the organization")
        }

        val detachedIds = mutableListOf<UUID>()
        providerEntities.forEach { providerEntity ->
            // Find and delete the relationship if it exists
            val existing = EventProviderEntity.find {
                (EventProvidersTable.eventId eq eventEntity.id) and
                    (EventProvidersTable.providerId eq providerEntity.id)
            }.firstOrNull()

            if (existing != null) {
                existing.delete()
                detachedIds.add(providerEntity.id.value)
            }
        }

        detachedIds
    }

    override fun create(input: CreateProvider, orgSlug: String): UUID = transaction {
        val organisation = OrganisationEntity.orgFindBySlug(orgSlug)
            ?: throw NotFoundException("Organisation not found")

        val entity = ProviderEntity.new {
            name = input.name
            type = input.type
            website = input.website
            phone = input.phone
            email = input.email
            this.organisation = organisation
        }
        entity.id.value
    }

    override fun update(
        providerId: UUID,
        input: UpdateProvider,
        orgSlug: String,
    ): Provider = transaction {
        val organisation = OrganisationEntity.orgFindBySlug(orgSlug)
            ?: throw NotFoundException("Organisation not found")

        ProviderEntity
            .find { (ProvidersTable.id eq providerId) and (ProvidersTable.organisationId eq organisation.id) }
            .firstOrNull()
            ?.let { entity ->
                input.name?.let { entity.name = it }
                input.type?.let { entity.type = it }
                input.website?.let { entity.website = it }
                input.phone?.let { entity.phone = it }
                input.email?.let { entity.email = it }
                entity.toDomain()
            }
            ?: throw NotFoundException("Provider not found or not owned by organisation")
    }

    override fun delete(providerId: UUID, orgSlug: String): Boolean = transaction {
        val organisation = OrganisationEntity.orgFindBySlug(orgSlug)
            ?: throw NotFoundException("Organisation not found")

        ProviderEntity
            .find { (ProvidersTable.id eq providerId) and (ProvidersTable.organisationId eq organisation.id) }
            .firstOrNull()
            ?.let { entity ->
                entity.delete()
                true
            } ?: false
    }

    override fun hasEventAttachments(providerId: UUID): Boolean = transaction {
        EventProviderEntity.find { EventProvidersTable.providerId eq providerId }.count() > 0
    }
}
