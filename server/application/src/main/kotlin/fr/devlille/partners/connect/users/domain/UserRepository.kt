package fr.devlille.partners.connect.users.domain

import java.util.UUID

interface UserRepository {
    fun createUserIfNotExist(user: User)

    fun findUsersByEventId(eventId: UUID): List<User>

    fun hasEditPermissionByEmail(email: String, eventId: UUID): Boolean

    fun grantUsers(eventId: UUID, userEmails: List<String>)
}
