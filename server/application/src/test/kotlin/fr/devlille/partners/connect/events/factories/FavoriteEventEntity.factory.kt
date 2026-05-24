package fr.devlille.partners.connect.events.factories

import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.events.infrastructure.db.FavoriteEventEntity
import fr.devlille.partners.connect.users.infrastructure.db.UserEntity
import java.util.UUID

fun insertMockedFavoriteEvent(
    userId: UUID,
    eventId: UUID,
): FavoriteEventEntity = FavoriteEventEntity.new {
    this.user = UserEntity[userId]
    this.event = EventEntity[eventId]
}
