package fr.devlille.partners.connect.organisations.infrastructure.db

import fr.devlille.partners.connect.users.infrastructure.db.UserEntity
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import java.util.UUID

class OrganisationEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<OrganisationEntity>(OrganisationsTable)

    var name by OrganisationsTable.name
    var slug by OrganisationsTable.slug
    var headOffice by OrganisationsTable.headOffice
    var siret by OrganisationsTable.siret
    var siren by OrganisationsTable.siren
    var tva by OrganisationsTable.tva
    var dAndB by OrganisationsTable.dAndB
    var nace by OrganisationsTable.nace
    var naf by OrganisationsTable.naf
    var duns by OrganisationsTable.duns
    var iban by OrganisationsTable.iban
    var bic by OrganisationsTable.bic
    var ribUrl by OrganisationsTable.ribUrl
    var creationLocation by OrganisationsTable.creationLocation
    var createdAt by OrganisationsTable.createdAt
    var publishedAt by OrganisationsTable.publishedAt

    var representativeUser by UserEntity referencedOn OrganisationsTable.representativeUser
    var representativeRole by OrganisationsTable.representativeRole
}

fun UUIDEntityClass<OrganisationEntity>.findBySlug(slug: String): OrganisationEntity? = this
    .find { OrganisationsTable.slug eq slug }
    .singleOrNull()
