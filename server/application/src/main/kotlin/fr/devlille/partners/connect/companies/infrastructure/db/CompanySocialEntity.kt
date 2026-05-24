package fr.devlille.partners.connect.companies.infrastructure.db

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.dao.java.UUIDEntity
import org.jetbrains.exposed.v1.dao.java.UUIDEntityClass
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.util.UUID

class CompanySocialEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<CompanySocialEntity>(CompanySocialsTable) {
        fun deleteAllByCompanyId(companyId: UUID): Unit = this
            .find { CompanySocialsTable.companyId eq companyId }
            .forEach { it.delete() }

        fun countByCompanies(companyIds: Set<UUID>): Map<UUID, Int> = CompanySocialsTable
            .selectAll()
            .where { CompanySocialsTable.companyId inList companyIds }
            .groupingBy { it[CompanySocialsTable.companyId].value }
            .eachCount()
    }

    var company by CompanyEntity referencedOn CompanySocialsTable.companyId
    var type by CompanySocialsTable.type
    var url by CompanySocialsTable.url
}
