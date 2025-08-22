package fr.devlille.partners.connect.provider.application

import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.events.infrastructure.db.findBySlug
import fr.devlille.partners.connect.provider.domain.CreateProvider
import fr.devlille.partners.connect.provider.domain.Provider
import fr.devlille.partners.connect.provider.domain.ProviderRepository
import fr.devlille.partners.connect.provider.infrastructure.db.EventProviderEntity
import fr.devlille.partners.connect.provider.infrastructure.db.EventProvidersTable
import fr.devlille.partners.connect.provider.infrastructure.db.ProviderEntity
import fr.devlille.partners.connect.provider.infrastructure.db.ProvidersTable
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

class ProviderRepositoryExposed : ProviderRepository {
    override fun list(query: String?, sort: String?, direction: String?): List<Provider> = transaction {
        var queryBuilder = ProviderEntity.all()

        // Apply search filter if query is provided
        query?.let { searchQuery ->
            queryBuilder = ProviderEntity.find {
                ProvidersTable.name.lowerCase() like "%${searchQuery.lowercase()}%"
            }
        }

        // Apply sorting
        val sortOrder = when (direction?.lowercase()) {
            "desc" -> SortOrder.DESC
            else -> SortOrder.ASC // default to ASC
        }

        val sortedQuery = when (sort?.lowercase()) {
            "creation", "created", "createdat" -> queryBuilder.orderBy(ProvidersTable.createdAt to sortOrder)
            "name" -> queryBuilder.orderBy(ProvidersTable.name to sortOrder)
            else -> queryBuilder.orderBy(ProvidersTable.createdAt to sortOrder) // default to createdAt
        }

        sortedQuery.map { entity ->
            Provider(
                id = entity.id.value.toString(),
                name = entity.name,
                type = entity.type,
                website = entity.website,
                phone = entity.phone,
                email = entity.email,
                createdAt = entity.createdAt,
            )
        }
    }

    override fun create(input: CreateProvider): UUID = transaction {
        val entity = ProviderEntity.new {
            name = input.name
            type = input.type
            website = input.website
            phone = input.phone
            email = input.email
        }

        entity.id.value
    }

    override fun attachToEvent(eventSlug: String, providerIds: List<UUID>): List<UUID> = transaction {
        if (providerIds.isEmpty()) {
            throw BadRequestException("Provider IDs list cannot be empty")
        }

        val eventEntity = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")

        // Validate that all provider IDs exist and get provider entities
        val providerEntities = ProviderEntity.find {
            ProvidersTable.id inList providerIds
        }.toList()

        if (providerEntities.size != providerIds.size) {
            throw BadRequestException("One or more provider IDs do not exist")
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
                    eventId = eventEntity.id
                    providerId = providerEntity.id
                }
            }
            attachedIds.add(providerEntity.id.value)
        }

        attachedIds
    }

    override fun detachFromEvent(eventSlug: String, providerIds: List<UUID>): List<UUID> = transaction {
        if (providerIds.isEmpty()) {
            throw BadRequestException("Provider IDs list cannot be empty")
        }

        val eventEntity = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")

        // Validate that all provider IDs exist
        val providerEntities = ProviderEntity.find {
            ProvidersTable.id inList providerIds
        }.toList()

        if (providerEntities.size != providerIds.size) {
            throw BadRequestException("One or more provider IDs do not exist")
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
}
