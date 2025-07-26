package fr.devlille.partners.connect.companies.infrastructure.db

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import java.util.UUID

class InvoiceEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<InvoiceEntity>(InvoicesTable)

    var company by CompanyEntity referencedOn InvoicesTable.companyId
    var name by InvoicesTable.name
    var contactFirstName by InvoicesTable.contactFirstName
    var contactSecondName by InvoicesTable.contactSecondName
    var contactEmail by InvoicesTable.contactEmail
    var address by InvoicesTable.address
    var city by InvoicesTable.city
    var zipCode by InvoicesTable.zipCode
    var country by InvoicesTable.country
    var siret by InvoicesTable.siret
    var vat by InvoicesTable.vat
    var po by InvoicesTable.po
    var invoicePdfUrl by InvoicesTable.invoicePdfUrl
    var status by InvoicesTable.status
    var createdAt by InvoicesTable.createdAt
}
