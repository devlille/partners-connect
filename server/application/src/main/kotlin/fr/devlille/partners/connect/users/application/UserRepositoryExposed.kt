package fr.devlille.partners.connect.users.application

import fr.devlille.partners.connect.internal.infrastructure.api.ConflictException
import fr.devlille.partners.connect.organisations.infrastructure.db.OrganisationEntity
import fr.devlille.partners.connect.organisations.infrastructure.db.findBySlug
import fr.devlille.partners.connect.users.domain.RevokeUsersResult
import fr.devlille.partners.connect.users.domain.User
import fr.devlille.partners.connect.users.domain.UserRepository
import fr.devlille.partners.connect.users.infrastructure.db.OrganisationPermissionEntity
import fr.devlille.partners.connect.users.infrastructure.db.OrganisationPermissionsTable
import fr.devlille.partners.connect.users.infrastructure.db.UserEntity
import fr.devlille.partners.connect.users.infrastructure.db.hasPermission
import fr.devlille.partners.connect.users.infrastructure.db.listUserGrantedByOrgId
import fr.devlille.partners.connect.users.infrastructure.db.singleEventPermission
import fr.devlille.partners.connect.users.infrastructure.db.singleUserByEmail
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

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

    override fun findUsersByOrgSlug(orgSlug: String): List<User> = transaction {
        val organisation = OrganisationEntity.findBySlug(orgSlug)
            ?: throw NotFoundException("Organisation with slug: $orgSlug not found")
        OrganisationPermissionEntity
            .listUserGrantedByOrgId(organisation.id.value)
            .map { it.user.toDomain() }
    }

    override fun hasEditPermissionByEmail(email: String, orgSlug: String): Boolean = transaction {
        val user = UserEntity.singleUserByEmail(email)
            ?: throw NotFoundException("User with email $email not found")
        val organisation = OrganisationEntity.findBySlug(orgSlug)
            ?: throw NotFoundException("Organisation with slug: $orgSlug not found")
        OrganisationPermissionEntity
            .hasPermission(organisationId = organisation.id.value, userId = user.id.value)
    }

    override fun grantUsers(orgSlug: String, userEmails: List<String>) = transaction {
        val org = OrganisationEntity.findBySlug(orgSlug)
            ?: throw NotFoundException("Organisation with slug: $orgSlug not found")
        userEmails.forEach { userEmail ->
            val userEntity = UserEntity
                .singleUserByEmail(userEmail)
                ?: throw NotFoundException("User with email: $userEmail not found")
            val existing = OrganisationPermissionEntity
                .singleEventPermission(organisationId = org.id.value, userId = userEntity.id.value)
            if (existing != null) {
                existing.canEdit = true
            } else {
                OrganisationPermissionEntity.new {
                    this.organisation = org
                    this.user = userEntity
                    this.canEdit = true
                }
            }
        }
    }

    override fun revokeUsers(
        orgSlug: String,
        userEmails: List<String>,
        requestingUserEmail: String,
    ): RevokeUsersResult = transaction {
        val org = OrganisationEntity.findBySlug(orgSlug)
            ?: throw NotFoundException("Organisation with slug: $orgSlug not found")

        // Deduplicate emails
        val uniqueEmails = userEmails.distinct()

        // Check self-revocation + last editor constraint
        if (requestingUserEmail in uniqueEmails) {
            val editorCount = OrganisationPermissionEntity
                .find {
                    (OrganisationPermissionsTable.organisationId eq org.id) and
                        (OrganisationPermissionsTable.canEdit eq true)
                }
                .count()

            if (editorCount == 1L) {
                throw ConflictException(
                    "Cannot revoke your own access as the last editor of this organisation",
                )
            }
        }

        // Process each email
        val notFoundEmails = mutableListOf<String>()
        var revokedCount = 0

        uniqueEmails.forEach { email ->
            val userEntity = UserEntity.singleUserByEmail(email)

            if (userEntity == null) {
                notFoundEmails.add(email)
            } else {
                val permission = OrganisationPermissionEntity
                    .singleEventPermission(
                        organisationId = org.id.value,
                        userId = userEntity.id.value,
                    )

                if (permission != null && permission.canEdit) {
                    permission.delete()
                    revokedCount++
                } else if (permission == null) {
                    notFoundEmails.add(email)
                }
            }
        }

        RevokeUsersResult(
            revokedCount = revokedCount,
            notFoundEmails = notFoundEmails,
        )
    }

    override fun getByEmail(email: String): User? = transaction {
        UserEntity.singleUserByEmail(email)?.toDomain()
    }
}
