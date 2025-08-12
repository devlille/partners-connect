package fr.devlille.partners.connect.users.infrastructure.db

import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import org.jetbrains.exposed.v1.jdbc.SizedIterable
import java.util.UUID

class EventPermissionEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<EventPermissionEntity>(EventPermissionsTable)

    var event by EventEntity referencedOn EventPermissionsTable.eventId
    var user by UserEntity referencedOn EventPermissionsTable.userId
    var canEdit by EventPermissionsTable.canEdit
}

fun UUIDEntityClass<EventPermissionEntity>.listUserGrantedByEvent(eventId: UUID): SizedIterable<EventPermissionEntity> =
    this.find { (EventPermissionsTable.eventId eq eventId) and (EventPermissionsTable.canEdit eq true) }

fun UUIDEntityClass<EventPermissionEntity>.singleEventPermission(
    eventId: UUID,
    userId: UUID,
): EventPermissionEntity? = this
    .find { (EventPermissionsTable.eventId eq eventId) and (EventPermissionsTable.userId eq userId) }
    .singleOrNull()

fun UUIDEntityClass<EventPermissionEntity>.hasPermission(eventId: UUID, userId: UUID): Boolean = this
    .find {
        (EventPermissionsTable.eventId eq eventId) and
            (EventPermissionsTable.canEdit eq true) and
            (EventPermissionsTable.userId eq userId)
    }
    .empty().not()
