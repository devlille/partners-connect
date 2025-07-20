package fr.devlille.partners.connect.companies.infrastructure.db

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import java.util.UUID

class CompanyEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<CompanyEntity>(CompaniesTable)

    var name by CompaniesTable.name
    var description by CompaniesTable.description
    var siteUrl by CompaniesTable.siteUrl
    var logoUrlOriginal by CompaniesTable.logoUrlOriginal
    var logoUrl1000 by CompaniesTable.logoUrl1000
    var logoUrl500 by CompaniesTable.logoUrl500
    var logoUrl250 by CompaniesTable.logoUrl250
    var createdAt by CompaniesTable.createdAt
}
