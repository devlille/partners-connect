package fr.devlille.partners.connect.companies.factories

import fr.devlille.partners.connect.companies.infrastructure.db.CompanyEntity
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

@Suppress("LongParameterList")
fun insertMockedCompany(
    id: UUID = UUID.randomUUID(),
    name: String = "Mock Company",
    address: String = "123 Mock St",
    city: String = "Mock City",
    zipCode: String = "12345",
    country: String = "MO",
    siret: String = "12345678901234",
    vat: String = "FR12345678901",
    description: String? = "This is a mock company for testing purposes.",
    siteUrl: String = "https://www.mockcompany.com",
): CompanyEntity = transaction {
    CompanyEntity.new(id) {
        this.name = name
        this.address = address
        this.city = city
        this.zipCode = zipCode
        this.country = country
        this.siret = siret
        this.vat = vat
        this.description = description
        this.siteUrl = siteUrl
        this.logoUrlOriginal = null
        this.logoUrl1000 = null
        this.logoUrl500 = null
        this.logoUrl250 = null
    }
}
