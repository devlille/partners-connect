package fr.devlille.partners.connect.organisations.application

import fr.devlille.partners.connect.internal.infrastructure.api.BadRequestException
import fr.devlille.partners.connect.internal.infrastructure.api.ErrorCode
import fr.devlille.partners.connect.internal.infrastructure.api.MetaKeys
import fr.devlille.partners.connect.internal.infrastructure.api.NotFoundException
import fr.devlille.partners.connect.internal.infrastructure.slugify.slugify
import fr.devlille.partners.connect.organisations.application.mappers.toDomain
import fr.devlille.partners.connect.organisations.application.mappers.toItemDomain
import fr.devlille.partners.connect.organisations.domain.Organisation
import fr.devlille.partners.connect.organisations.domain.OrganisationItem
import fr.devlille.partners.connect.organisations.domain.OrganisationRepository
import fr.devlille.partners.connect.organisations.infrastructure.db.OrganisationEntity
import fr.devlille.partners.connect.organisations.infrastructure.db.findBySlug
import fr.devlille.partners.connect.users.infrastructure.db.OrganisationPermissionEntity
import fr.devlille.partners.connect.users.infrastructure.db.OrganisationPermissionsTable
import fr.devlille.partners.connect.users.infrastructure.db.UserEntity
import fr.devlille.partners.connect.users.infrastructure.db.singleUserByEmail
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class OrganisationRepositoryExposed : OrganisationRepository {
    override fun create(entity: Organisation): String = transaction {
        // Validate that name is present and not blank
        if (entity.name.isBlank()) {
            throw BadRequestException(
                message = "Organisation name is required and cannot be empty",
            )
        }

        val user = entity.representativeUserEmail?.let { email ->
            UserEntity.singleUserByEmail(email)
                ?: throw NotFoundException(
                    code = ErrorCode.USER_NOT_FOUND,
                    message = "User with email $email not found",
                    meta = mapOf(MetaKeys.EMAIL to email),
                )
        }
        val slug = entity.name.slugify()
        OrganisationEntity.new {
            this.name = entity.name
            this.slug = slug
            this.headOffice = entity.headOffice
            this.siret = entity.siret
            this.siren = entity.siren
            this.tva = entity.tva
            this.dAndB = entity.dAndB
            this.nace = entity.nace
            this.naf = entity.naf
            this.duns = entity.duns
            this.iban = entity.iban
            this.bic = entity.bic
            this.ribUrl = entity.ribUrl
            this.creationLocation = entity.creationLocation
            this.createdAt = entity.createdAt
            this.publishedAt = entity.publishedAt
            this.representativeUser = user
            this.representativeRole = entity.representativeRole
        }
        slug
    }

    override fun getById(slug: String): Organisation = transaction {
        OrganisationEntity.findBySlug(slug)?.toDomain()
            ?: throw NotFoundException(
                code = ErrorCode.ORGANISATION_NOT_FOUND,
                message = "Organisation with slug $slug not found",
                meta = mapOf(MetaKeys.ORGANISATION to slug),
            )
    }

    override fun update(orgSlug: String, data: Organisation): Organisation = transaction {
        // Validate that name is present and not blank
        if (data.name.isBlank()) {
            throw BadRequestException(
                message = "Organisation name is required and cannot be empty",
            )
        }

        val entity = OrganisationEntity.findBySlug(orgSlug)
            ?: throw NotFoundException(
                code = ErrorCode.ORGANISATION_NOT_FOUND,
                message = "Organisation with slug $orgSlug not found",
                meta = mapOf(MetaKeys.ORGANISATION to orgSlug),
            )

        val representativeUser = data.representativeUserEmail?.let { email ->
            UserEntity.singleUserByEmail(email)
                ?: throw NotFoundException(
                    code = ErrorCode.USER_NOT_FOUND,
                    message = "User with email $email not found",
                    meta = mapOf(MetaKeys.EMAIL to email),
                )
        }

        entity.apply {
            this.name = data.name
            this.headOffice = data.headOffice
            this.siret = data.siret
            this.siren = data.siren
            this.tva = data.tva
            this.dAndB = data.dAndB
            this.nace = data.nace
            this.naf = data.naf
            this.duns = data.duns
            this.iban = data.iban
            this.bic = data.bic
            this.ribUrl = data.ribUrl
            this.creationLocation = data.creationLocation
            this.createdAt = data.createdAt
            this.publishedAt = data.publishedAt
            this.representativeUser = representativeUser
            this.representativeRole = data.representativeRole
        }
        entity.toDomain()
    }

    override fun findOrganisationListByUserEmail(userEmail: String): List<OrganisationItem> = transaction {
        val user = UserEntity.singleUserByEmail(userEmail)
            ?: throw NotFoundException(
                code = ErrorCode.USER_NOT_FOUND,
                message = "User with email $userEmail not found",
                meta = mapOf(MetaKeys.EMAIL to userEmail),
            )

        OrganisationPermissionEntity
            .find {
                (OrganisationPermissionsTable.userId eq user.id.value) and
                    (OrganisationPermissionsTable.canEdit eq true)
            }
            .map { it.organisation.toItemDomain() }
    }
}
