package fr.devlille.partners.connect.events.infrastructure.db

import fr.devlille.partners.connect.users.infrastructure.db.UsersTable
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.datetime.datetime
import kotlin.time.Clock

object FavoriteEventsTable : UUIDTable("favorite_events") {
    val userId = reference("user_id", UsersTable)
    val eventId = reference("event_id", EventsTable)
    val favoritedAt = datetime("favorited_at").clientDefault {
        Clock.System.now().toLocalDateTime(TimeZone.UTC)
    }

    init {
        uniqueIndex(userId, eventId)
    }
}
