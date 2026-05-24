package fr.devlille.partners.connect.events.infrastructure.db

import fr.devlille.partners.connect.users.infrastructure.db.UserEntity
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.dao.java.UUIDEntity
import org.jetbrains.exposed.v1.dao.java.UUIDEntityClass
import java.util.UUID

class FavoriteEventEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<FavoriteEventEntity>(FavoriteEventsTable)

    var user by UserEntity referencedOn FavoriteEventsTable.userId
    var event by EventEntity referencedOn FavoriteEventsTable.eventId
    var favoritedAt by FavoriteEventsTable.favoritedAt
}

fun UUIDEntityClass<FavoriteEventEntity>.singleFavorite(
    userId: UUID,
    eventId: UUID,
): FavoriteEventEntity? = this.find {
    (FavoriteEventsTable.userId eq userId) and (FavoriteEventsTable.eventId eq eventId)
}.singleOrNull()

fun UUIDEntityClass<FavoriteEventEntity>.listByUserOrderByEventStartTime(
    userId: UUID,
): List<FavoriteEventEntity> = this
    .find { FavoriteEventsTable.userId eq userId }
    .sortedBy { it.event.startTime }
