package fr.devlille.partners.connect.organisations.application

import fr.devlille.partners.connect.organisations.application.mappers.toDomain
import fr.devlille.partners.connect.organisations.domain.Organisation
import fr.devlille.partners.connect.organisations.domain.OrganisationRepository
import fr.devlille.partners.connect.organisations.infrastructure.db.OrganisationEntity
import fr.devlille.partners.connect.users.infrastructure.db.UserEntity
import fr.devlille.partners.connect.users.infrastructure.db.singleUserByEmail
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

class OrganisationRepositoryExposed : OrganisationRepository {
    override fun create(entity: Organisation): UUID = transaction {
        val user = UserEntity.singleUserByEmail(entity.representativeUserEmail)
            ?: throw NotFoundException("User with email ${entity.representativeUserEmail} not found")
        val entity = OrganisationEntity.new {
            this.name = entity.name
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
        entity.id.value
    }

    override fun getById(id: UUID): Organisation = transaction {
        OrganisationEntity.findById(id)?.toDomain() ?: throw NotFoundException("Organisation with id $id not found")
    }
}
