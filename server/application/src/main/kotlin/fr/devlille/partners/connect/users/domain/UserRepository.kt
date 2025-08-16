package fr.devlille.partners.connect.users.domain

interface UserRepository {
    fun createUserIfNotExist(user: User)

    fun findUsersByOrgSlug(orgSlug: String): List<User>

    fun hasEditPermissionByEmail(email: String, orgSlug: String): Boolean

    fun grantUsers(orgSlug: String, userEmails: List<String>)

    fun getByEmail(email: String): User?
}
