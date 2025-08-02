package fr.devlille.partners.connect.legaentity.infrastructure.db

import fr.devlille.partners.connect.users.infrastructure.db.UserEntity
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import java.util.UUID

class LegalEntityEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<LegalEntityEntity>(LegalEntitiesTable)

    var name by LegalEntitiesTable.name
    var headOffice by LegalEntitiesTable.headOffice
    var siret by LegalEntitiesTable.siret
    var siren by LegalEntitiesTable.siren
    var tva by LegalEntitiesTable.tva
    var dAndB by LegalEntitiesTable.dAndB
    var nace by LegalEntitiesTable.nace
    var naf by LegalEntitiesTable.naf
    var duns by LegalEntitiesTable.duns
    var iban by LegalEntitiesTable.iban
    var bic by LegalEntitiesTable.bic
    var ribUrl by LegalEntitiesTable.ribUrl
    var creationLocation by LegalEntitiesTable.creationLocation
    var createdAt by LegalEntitiesTable.createdAt
    var publishedAt by LegalEntitiesTable.publishedAt

    var representativeUser by UserEntity referencedOn LegalEntitiesTable.representativeUser
    var representativeRole by LegalEntitiesTable.representativeRole
}
