package fr.devlille.partners.connect.provider.application

import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.provider.domain.CreateProvider
import fr.devlille.partners.connect.provider.domain.Provider
import fr.devlille.partners.connect.provider.domain.ProviderRepository
import fr.devlille.partners.connect.provider.infrastructure.db.EventProviderEntity
import fr.devlille.partners.connect.provider.infrastructure.db.EventProvidersTable
import fr.devlille.partners.connect.provider.infrastructure.db.ProviderEntity
import fr.devlille.partners.connect.provider.infrastructure.db.ProvidersTable
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import fr.devlille.partners.connect.events.infrastructure.db.findBySlug as eventFindBySlug

class ProviderRepositoryExposed : ProviderRepository {
    override fun list(): List<Provider> = transaction {
        ProviderEntity.all().map { entity ->
            Provider(
                id = entity.id.value.toString(),
                name = entity.name,
                type = entity.type,
                website = entity.website,
                phone = entity.phone,
                email = entity.email,
            )
        }
    }

    override fun create(input: CreateProvider): Provider = transaction {
        val entity = ProviderEntity.new {
            name = input.name
            type = input.type
            website = input.website
            phone = input.phone
            email = input.email
        }

        Provider(
            id = entity.id.value.toString(),
            name = entity.name,
            type = entity.type,
            website = entity.website,
            phone = entity.phone,
            email = entity.email,
        )
    }

    override fun attachToEvent(eventSlug: String, providerIds: List<UUID>): List<UUID> = transaction {
        val eventEntity = EventEntity.eventFindBySlug(eventSlug)
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

    override fun validateProviderIds(providerIds: List<UUID>): Boolean = transaction {
        val existingCount = ProviderEntity.find {
            ProvidersTable.id inList providerIds
        }.count()
        existingCount.toInt() == providerIds.size
    }
}
