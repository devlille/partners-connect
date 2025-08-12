package fr.devlille.partners.connect.users.infrastructure.db

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import java.util.UUID

class UserEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<UserEntity>(UsersTable)

    var email by UsersTable.email
    var name by UsersTable.name
    var pictureUrl by UsersTable.pictureUrl
}

fun UUIDEntityClass<UserEntity>.singleUserByEmail(email: String): UserEntity? = UserEntity
    .find { UsersTable.email eq email }
    .singleOrNull()
