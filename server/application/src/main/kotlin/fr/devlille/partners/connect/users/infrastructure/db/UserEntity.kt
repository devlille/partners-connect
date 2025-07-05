package fr.devlille.partners.connect.users.infrastructure.db

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.Entity
import org.jetbrains.exposed.v1.dao.EntityClass
import java.util.UUID

class UserEntity(id: EntityID<UUID>) : Entity<UUID>(id) {
    companion object : EntityClass<UUID, UserEntity>(UsersTable)

    var email by UsersTable.email
    var name by UsersTable.name
    var pictureUrl by UsersTable.pictureUrl
}
