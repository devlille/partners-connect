package fr.devlille.partners.connect.users.factories

import fr.devlille.partners.connect.users.infrastructure.db.UserEntity
import java.util.UUID

fun insertMockedUser(
    id: UUID = UUID.randomUUID(),
    email: String = "$id@mail.com",
    name: String? = "John Doe",
    pictureUrl: String = "https://example.com/picture.jpg",
): UserEntity = UserEntity.new(id) {
    this.email = email
    this.name = name
    this.pictureUrl = pictureUrl
}
