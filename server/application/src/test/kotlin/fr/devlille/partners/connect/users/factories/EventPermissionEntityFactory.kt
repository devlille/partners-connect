package fr.devlille.partners.connect.users.factories

import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.internal.insertMockedEvent
import fr.devlille.partners.connect.users.infrastructure.db.EventPermissionEntity
import fr.devlille.partners.connect.users.infrastructure.db.UserEntity
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

fun insertMockedEventPermission(
    event: EventEntity = insertMockedEvent(),
    user: UserEntity,
    canEdit: Boolean = true,
): EventPermissionEntity = transaction {
    EventPermissionEntity.new {
        this.event = event
        this.user = user
        this.canEdit = canEdit
    }
}
