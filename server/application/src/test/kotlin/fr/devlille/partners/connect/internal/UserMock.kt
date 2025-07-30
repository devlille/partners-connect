package fr.devlille.partners.connect.internal

import fr.devlille.partners.connect.legaentity.infrastructure.db.LegalEntityEntity
import fr.devlille.partners.connect.users.infrastructure.db.UserEntity
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

fun insertMockedEventWithAdminUser(
    eventId: UUID = UUID.randomUUID(),
    legalEntity: LegalEntityEntity = insertLegalEntity(),
) {
    insertMockedEventPermission(
        eventId = insertMockedEvent(eventId, legalEntity = legalEntity).id.value,
        user = insertMockedAdminUser(),
    )
}

fun insertMockedAdminUser(
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

fun insertMockedUser(
    id: UUID = UUID.randomUUID(),
    email: String = "$id@mail.com",
    name: String? = "John Doe",
    pictureUrl: String = "https://example.com/picture.jpg",
): UserEntity = transaction {
    UserEntity.new(id) {
        this.email = email
        this.name = name
        this.pictureUrl = pictureUrl
    }
}
