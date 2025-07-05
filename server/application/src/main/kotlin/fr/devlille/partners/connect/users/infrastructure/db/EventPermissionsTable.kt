package fr.devlille.partners.connect.users.infrastructure.db

import org.jetbrains.exposed.v1.core.dao.id.IntIdTable

object EventPermissionsTable : IntIdTable("event_permissions") {
    val eventId = uuid("event_id")
    val userId = reference("user_id", UsersTable)
    val canEdit = bool("can_edit").default(true)

    init {
        uniqueIndex(eventId, userId)
    }
}
