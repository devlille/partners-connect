package fr.devlille.partners.connect.users.infrastructure.db

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.Entity
import org.jetbrains.exposed.v1.dao.EntityClass

class EventPermissionEntity(id: EntityID<Int>) : Entity<Int>(id) {
    companion object : EntityClass<Int, EventPermissionEntity>(EventPermissionsTable)

    var eventId by EventPermissionsTable.eventId
    var user by UserEntity referencedOn EventPermissionsTable.userId
    var canEdit by EventPermissionsTable.canEdit
}
