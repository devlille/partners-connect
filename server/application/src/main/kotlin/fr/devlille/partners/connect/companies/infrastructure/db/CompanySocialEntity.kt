package fr.devlille.partners.connect.companies.infrastructure.db

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import java.util.UUID

class CompanySocialEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<CompanySocialEntity>(CompanySocialsTable)

    var company by CompanyEntity referencedOn CompanySocialsTable.companyId
    var type by CompanySocialsTable.type
    var url by CompanySocialsTable.url
}
