package fr.devlille.partners.connect.users.domain

interface UserRepository {
    fun createUserIfNotExist(user: User)

    fun findUsersByOrgSlug(orgSlug: String): List<User>

    fun hasEditPermissionByEmail(email: String, orgSlug: String): Boolean

    fun grantUsers(orgSlug: String, userEmails: List<String>)

    /**
     * Revokes edit permissions for specified users from an organisation.
     *
     * This method removes edit permissions for the provided users from the specified organisation.
     * The operation is idempotent - attempting to revoke permissions for users who don't have
     * permissions will not cause an error. Non-existent users are reported in the result.
     *
     * @param orgSlug The unique identifier (slug) of the organisation
     * @param userEmails List of email addresses identifying users whose permissions should be revoked
     * @param requestingUserEmail Email of the user requesting the revocation (used for self-revocation check)
     * @return RevokeUsersResult containing the count of revoked users and list of non-existent emails
     * @throws NotFoundException if the organisation with the given slug is not found
     * @throws ConflictException if the requesting user attempts to revoke their own access as the last editor
     */
    fun revokeUsers(orgSlug: String, userEmails: List<String>, requestingUserEmail: String): RevokeUsersResult

    fun getByEmail(email: String): User?
}
