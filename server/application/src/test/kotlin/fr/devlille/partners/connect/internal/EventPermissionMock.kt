package fr.devlille.partners.connect.internal

import fr.devlille.partners.connect.users.infrastructure.db.EventPermissionEntity
import fr.devlille.partners.connect.users.infrastructure.db.UserEntity
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

fun insertMockedEventPermission(
    eventId: UUID = UUID.randomUUID(),
    user: UserEntity,
    canEdit: Boolean = true,
): EventPermissionEntity = transaction {
    EventPermissionEntity.new {
        this.eventId = eventId
        this.user = user
        this.canEdit = canEdit
    }
}
