package fr.devlille.partners.connect.users.infrastructure.db

import fr.devlille.partners.connect.events.infrastructure.db.EventsTable
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable

object EventPermissionsTable : UUIDTable("event_permissions") {
    val eventId = reference("event_id", EventsTable)
    val userId = reference("user_id", UsersTable)
    val canEdit = bool("can_edit").default(true)

    init {
        uniqueIndex(eventId, userId)
    }
}
