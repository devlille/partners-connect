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
}
