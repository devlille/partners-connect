package fr.devlille.partners.connect.companies.infrastructure.db

import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import org.jetbrains.exposed.v1.jdbc.SizedIterable
import java.util.UUID

class CompanyEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<CompanyEntity>(CompaniesTable)

    var name by CompaniesTable.name
    var description by CompaniesTable.description
    var headOffice by CompaniesTable.headOffice
    var siret by CompaniesTable.siret
    var siteUrl by CompaniesTable.siteUrl
    var logoUrlOriginal by CompaniesTable.logoUrlOriginal
    var logoUrl1000 by CompaniesTable.logoUrl1000
    var logoUrl500 by CompaniesTable.logoUrl500
    var logoUrl250 by CompaniesTable.logoUrl250
    var createdAt by CompaniesTable.createdAt
}

fun UUIDEntityClass<CompanyEntity>.listByQuery(query: String?): SizedIterable<CompanyEntity> {
    val companies = if (query.isNullOrBlank()) {
        this.all()
    } else {
        this.find { CompaniesTable.name.lowerCase() like "%${query.lowercase()}%" }
    }
    return companies.orderBy(CompaniesTable.name to SortOrder.ASC)
}
