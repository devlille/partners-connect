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
    override fun createUserIfNotExist(user: User) {
        transaction {
            if (UserEntity.find { usersTable.email eq user.email }.singleOrNull() == null) {
                UserEntity.new {
                    this.name = user.displayName
                    this.email = user.email
                    this.pictureUrl = user.pictureUrl
                }
            }
        }
    }

    override fun findUsersByEventId(eventId: UUID): List<User> = transaction {
        EventPermissionEntity
            .find {
                (permsTable.eventId eq eventId) and (permsTable.canEdit eq true)
            }
            .map { it.user.toDomain() }
    }

    override fun hasEditPermissionByEmail(email: String, eventId: UUID): Boolean = transaction {
        val user = UserEntity
            .find { usersTable.email eq email }
            .firstOrNull()
            ?: throw NotFoundException("User with email $email not found")
        EventPermissionEntity
            .find {
                (permsTable.eventId eq eventId) and (permsTable.canEdit eq true) and (permsTable.userId eq user.id)
            }
            .empty().not()
    }

    override fun grantUsers(eventId: UUID, userEmails: List<String>) = transaction {
        userEmails.forEach { userEmail ->
            val userEntity = UserEntity
                .find { UsersTable.email eq userEmail }
                .singleOrNull()
                ?: throw NotFoundException("User with email: $userEmail not found")

            val existing = EventPermissionEntity
                .find { (permsTable.eventId eq eventId) and (permsTable.userId eq userEntity.id) }
                .firstOrNull()

            if (existing != null) {
                existing.canEdit = true
            } else {
                EventPermissionEntity.new {
                    this.eventId = eventId
                    this.user = userEntity
                    this.canEdit = true
                }
            }
        }
    }
}
