package fr.devlille.partners.connect.users.infrastructure.db

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.dao.java.UUIDEntity
import org.jetbrains.exposed.v1.dao.java.UUIDEntityClass
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
