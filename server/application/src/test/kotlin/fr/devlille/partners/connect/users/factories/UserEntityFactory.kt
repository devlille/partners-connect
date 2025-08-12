package fr.devlille.partners.connect.users.factories

import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.internal.insertMockedEvent
import fr.devlille.partners.connect.internal.mockedAdminUser
import fr.devlille.partners.connect.legaentity.infrastructure.db.LegalEntityEntity
import fr.devlille.partners.connect.legalentity.factories.insertLegalEntity
import fr.devlille.partners.connect.users.infrastructure.db.UserEntity
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

fun insertMockedEventWithAdminUser(
    eventId: UUID = UUID.randomUUID(),
    legalEntity: LegalEntityEntity = insertLegalEntity(),
): EventEntity {
    val mockedEvent = insertMockedEvent(eventId, legalEntity = legalEntity)
    insertMockedEventPermission(
        event = mockedEvent,
        user = insertMockedAdminUser(),
    )
    return mockedEvent
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
