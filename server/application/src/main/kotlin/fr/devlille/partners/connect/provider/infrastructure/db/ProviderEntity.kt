package fr.devlille.partners.connect.provider.infrastructure.db

import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.organisations.infrastructure.db.OrganisationEntity
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import java.util.UUID

/**
 * Exposed ORM entity for provider database records.
 *
 * Represents a provider (service or product provider) within the database using Exposed ORM.
 * Providers are organisation-scoped entities that can be attached to multiple events
 * for partnership management. This entity provides the database mapping layer between
 * the ProvidersTable schema and domain objects.
 *
 * Key relationships:
 * - Belongs to exactly one organisation (many-to-one)
 * - Can be attached to multiple events (many-to-many via EventProvidersTable)
 *
 * Usage:
 * ```kotlin
 * val provider = ProviderEntity.new {
 *     name = "Example Provider"
 *     type = "service"
 *     organisation = organisationEntity
 * }
 * ```
 */
class ProviderEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<ProviderEntity>(ProvidersTable)

    /** Display name of the provider */
    var name by ProvidersTable.name

    /** Category or type of provider */
    var type by ProvidersTable.type

    /** Optional website URL */
    var website by ProvidersTable.website

    /** Optional contact phone number */
    var phone by ProvidersTable.phone

    /** Optional contact email address */
    var email by ProvidersTable.email

    /** Owning organisation (required relationship) */
    var organisation by OrganisationEntity referencedOn ProvidersTable.organisationId

    /** Timestamp when provider was created */
    var createdAt by ProvidersTable.createdAt

    /** Many-to-many relationship with events via EventProvidersTable */
    val events by EventEntity via EventProvidersTable
}
