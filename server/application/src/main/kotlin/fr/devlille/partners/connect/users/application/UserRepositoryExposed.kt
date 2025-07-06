package fr.devlille.partners.connect.users.application

import fr.devlille.partners.connect.users.domain.User
import fr.devlille.partners.connect.users.domain.UserRepository
import fr.devlille.partners.connect.users.infrastructure.db.EventPermissionEntity
import fr.devlille.partners.connect.users.infrastructure.db.EventPermissionsTable
import fr.devlille.partners.connect.users.infrastructure.db.UserEntity
import fr.devlille.partners.connect.users.infrastructure.db.UsersTable
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

class UserRepositoryExposed(
    private val usersTable: UsersTable,
    private val permsTable: EventPermissionsTable,
) : UserRepository {
    override fun createUser(user: User) {
        transaction {
            UserEntity.new {
                this.name = user.displayName
                this.email = user.email
                this.pictureUrl = user.pictureUrl
            }
        }
    }

    override fun findUsersByEventId(eventId: String): List<User> = transaction {
        val eventUUID = UUID.fromString(eventId)
        EventPermissionEntity
            .find {
                (permsTable.eventId eq eventUUID) and (permsTable.canEdit eq true)
            }
            .map { it.user.toDomain() }
    }

    override fun hasEditPermissionByEmail(email: String, eventId: String): Boolean = transaction {
        val eventUUID = UUID.fromString(eventId)
        val user = UserEntity
            .find { usersTable.email eq email }
            .firstOrNull()
            ?: throw NotFoundException("User with email $email not found")
        EventPermissionEntity
            .find {
                (permsTable.eventId eq eventUUID) and (permsTable.canEdit eq true) and (permsTable.userId eq user.id)
            }
            .empty().not()
    }

    override fun grantUsers(eventId: String, userIds: List<String>) = transaction {
        val eventUUID = UUID.fromString(eventId)
        userIds.forEach { userId ->
            val userUUID = UUID.fromString(userId)
            val userEntity = UserEntity.findById(userUUID)
                ?: throw NotFoundException("User with id: $userId not found")

            val existing = EventPermissionEntity
                .find { (permsTable.eventId eq eventUUID) and (permsTable.userId eq userUUID) }
                .firstOrNull()

            if (existing != null) {
                existing.canEdit = true
            } else {
                EventPermissionEntity.new {
                    this.eventId = eventUUID
                    this.user = userEntity
                    this.canEdit = true
                }
            }
        }
    }
}
