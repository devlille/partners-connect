package fr.devlille.partners.connect.provider.infrastructure.db

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import java.util.UUID

class ProviderEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<ProviderEntity>(ProvidersTable)

    var name by ProvidersTable.name
    var type by ProvidersTable.type
    var website by ProvidersTable.website
    var phone by ProvidersTable.phone
    var email by ProvidersTable.email
    var createdAt by ProvidersTable.createdAt
}
