package fr.devlille.partners.connect.users.application

import fr.devlille.partners.connect.organisations.infrastructure.db.OrganisationEntity
import fr.devlille.partners.connect.organisations.infrastructure.db.findBySlug
import fr.devlille.partners.connect.users.domain.User
import fr.devlille.partners.connect.users.domain.UserRepository
import fr.devlille.partners.connect.users.infrastructure.db.OrganisationPermissionEntity
import fr.devlille.partners.connect.users.infrastructure.db.UserEntity
import fr.devlille.partners.connect.users.infrastructure.db.hasPermission
import fr.devlille.partners.connect.users.infrastructure.db.listUserGrantedByOrgId
import fr.devlille.partners.connect.users.infrastructure.db.singleEventPermission
import fr.devlille.partners.connect.users.infrastructure.db.singleUserByEmail
import io.ktor.server.plugins.NotFoundException
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

    override fun getByEmail(email: String): User? = transaction {
        UserEntity.singleUserByEmail(email)?.toDomain()
    }
}
