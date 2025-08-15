package fr.devlille.partners.connect.legaentity.application

import fr.devlille.partners.connect.legaentity.application.mappers.toDomain
import fr.devlille.partners.connect.legaentity.domain.LegalEntity
import fr.devlille.partners.connect.legaentity.domain.LegalEntityRepository
import fr.devlille.partners.connect.legaentity.infrastructure.db.LegalEntityEntity
import fr.devlille.partners.connect.users.infrastructure.db.UserEntity
import fr.devlille.partners.connect.users.infrastructure.db.singleUserByEmail
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

class LegalEntityRepositoryExposed : LegalEntityRepository {
    override fun create(entity: LegalEntity): UUID = transaction {
        val user = UserEntity.singleUserByEmail(entity.representativeUserEmail)
            ?: throw NotFoundException("User with email ${entity.representativeUserEmail} not found")
        val entity = LegalEntityEntity.new {
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

    override fun getById(id: UUID): LegalEntity = transaction {
        LegalEntityEntity.findById(id)?.toDomain() ?: throw NotFoundException("LegalEntity with id $id not found")
    }

    override fun update(id: UUID, entity: LegalEntity): LegalEntity = transaction {
        val user = UserEntity.singleUserByEmail(entity.representativeUserEmail)
            ?: throw NotFoundException("User with email ${entity.representativeUserEmail} not found")
        val existingEntity = LegalEntityEntity.findById(id)
            ?: throw NotFoundException("LegalEntity with id $id not found")

        existingEntity.apply {
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

        existingEntity.toDomain()
    }
}
