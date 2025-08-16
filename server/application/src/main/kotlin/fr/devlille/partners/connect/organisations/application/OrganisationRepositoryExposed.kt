package fr.devlille.partners.connect.organisations.application

import fr.devlille.partners.connect.internal.infrastructure.slugify.slugify
import fr.devlille.partners.connect.organisations.application.mappers.toDomain
import fr.devlille.partners.connect.organisations.domain.Organisation
import fr.devlille.partners.connect.organisations.domain.OrganisationRepository
import fr.devlille.partners.connect.organisations.infrastructure.db.OrganisationEntity
import fr.devlille.partners.connect.organisations.infrastructure.db.findBySlug
import fr.devlille.partners.connect.users.infrastructure.db.UserEntity
import fr.devlille.partners.connect.users.infrastructure.db.singleUserByEmail
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class OrganisationRepositoryExposed : OrganisationRepository {
    override fun create(entity: Organisation): String = transaction {
        val user = UserEntity.singleUserByEmail(entity.representativeUserEmail)
            ?: throw NotFoundException("User with email ${entity.representativeUserEmail} not found")
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
            ?: throw NotFoundException("Organisation with slug $slug not found")
    }

    override fun update(orgSlug: String, data: Organisation): Organisation? = transaction {
        val entity = OrganisationEntity.findBySlug(orgSlug) ?: return@transaction null

        val representativeUser = UserEntity.singleUserByEmail(data.representativeUserEmail)
            ?: throw NotFoundException("User with email ${data.representativeUserEmail} not found")

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
}
