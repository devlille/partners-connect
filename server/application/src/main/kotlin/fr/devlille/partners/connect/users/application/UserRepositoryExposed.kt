package fr.devlille.partners.connect.users.application

import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.users.domain.User
import fr.devlille.partners.connect.users.domain.UserRepository
import fr.devlille.partners.connect.users.infrastructure.db.EventPermissionEntity
import fr.devlille.partners.connect.users.infrastructure.db.UserEntity
import fr.devlille.partners.connect.users.infrastructure.db.hasPermission
import fr.devlille.partners.connect.users.infrastructure.db.listUserGrantedByEvent
import fr.devlille.partners.connect.users.infrastructure.db.singleEventPermission
import fr.devlille.partners.connect.users.infrastructure.db.singleUserByEmail
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

class UserRepositoryExposed : UserRepository {
    override fun createUserIfNotExist(user: User) {
        transaction {
            if (UserEntity.singleUserByEmail(user.email) == null) {
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
            .listUserGrantedByEvent(eventId)
            .map { it.user.toDomain() }
    }

    override fun hasEditPermissionByEmail(email: String, eventId: UUID): Boolean = transaction {
        val user = UserEntity.singleUserByEmail(email)
            ?: throw NotFoundException("User with email $email not found")
        EventPermissionEntity
            .hasPermission(eventId = eventId, userId = user.id.value)
    }

    override fun grantUsers(eventId: UUID, userEmails: List<String>) = transaction {
        val event = EventEntity.findById(eventId) ?: throw NotFoundException("Event with ID: $eventId not found")
        userEmails.forEach { userEmail ->
            val userEntity = UserEntity
                .singleUserByEmail(userEmail)
                ?: throw NotFoundException("User with email: $userEmail not found")
            val existing = EventPermissionEntity.singleEventPermission(eventId = eventId, userId = userEntity.id.value)
            if (existing != null) {
                existing.canEdit = true
            } else {
                EventPermissionEntity.new {
                    this.event = event
                    this.user = userEntity
                    this.canEdit = true
                }
            }
        }
    }
}
