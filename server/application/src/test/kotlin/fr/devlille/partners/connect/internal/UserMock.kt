package fr.devlille.partners.connect.internal

import fr.devlille.partners.connect.users.infrastructure.db.UserEntity
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

fun insertMockedAdminUser(
    eventId: UUID = UUID.randomUUID(),
) {
    insertMockedEventPermission(
        eventId = insertMockedEvent(eventId).id.value,
        user = insertMockedUser(),
    )
}

fun insertMockedUser(
    id: UUID = UUID.randomUUID(),
    email: String = mockedAdminUser.email,
    name: String = mockedAdminUser.name,
    pictureUrl: String = mockedAdminUser.picture,
): UserEntity = transaction {
    UserEntity.new(id) {
        this.email = email
        this.name = name
        this.pictureUrl = pictureUrl
    }
}
