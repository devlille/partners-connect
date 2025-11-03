package fr.devlille.partners.connect.companies.infrastructure.db

import fr.devlille.partners.connect.companies.domain.CompanyStatus
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import org.jetbrains.exposed.v1.jdbc.SizedIterable
import java.util.UUID

class CompanyEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<CompanyEntity>(CompaniesTable) {
        fun listByQueryAndStatus(query: String?, status: CompanyStatus?): SizedIterable<CompanyEntity> {
            val companies = if (query.isNullOrBlank() && status == null) {
                // Default: all companies
                all()
            } else if (query.isNullOrBlank() && status != null) {
                // Filter by status only
                find { CompaniesTable.status eq status }
            } else if (!query.isNullOrBlank() && status == null) {
                // Search query only (existing behavior)
                find { CompaniesTable.name.lowerCase() like "%${query.lowercase()}%" }
            } else {
                // Both query and status
                find {
                    val query = query!!.lowercase()
                    (CompaniesTable.name.lowerCase() like "%$query%") and (CompaniesTable.status eq status!!)
                }
            }
            return companies.orderBy(CompaniesTable.name to SortOrder.ASC)
        }
    }

    var name by CompaniesTable.name
    var description by CompaniesTable.description
    var address by CompaniesTable.address
    var city by CompaniesTable.city
    var zipCode by CompaniesTable.zipCode
    var country by CompaniesTable.country
    var siret by CompaniesTable.siret
    var vat by CompaniesTable.vat
    var siteUrl by CompaniesTable.siteUrl
    var logoUrlOriginal by CompaniesTable.logoUrlOriginal
    var logoUrl1000 by CompaniesTable.logoUrl1000
    var logoUrl500 by CompaniesTable.logoUrl500
    var logoUrl250 by CompaniesTable.logoUrl250
    var createdAt by CompaniesTable.createdAt
    var status by CompaniesTable.status
}
