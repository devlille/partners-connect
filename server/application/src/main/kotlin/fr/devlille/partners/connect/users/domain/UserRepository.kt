package fr.devlille.partners.connect.users.domain

interface UserRepository {
    fun createUser(user: User)
    fun findUsersByEventId(eventId: String): List<User>
    fun hasEditPermissionByEmail(email: String, eventId: String): Boolean
    fun grantUsers(eventId: String, userIds: List<String>)
}
