package fr.devlille.partners.connect.internal

import fr.devlille.partners.connect.companies.infrastructure.db.CompanyEntity
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

@Suppress("LongParameterList")
fun insertMockCompany(
    id: UUID = UUID.randomUUID(),
    name: String = "Mock Company",
    headOffice: String = "123 Mock St, Mock City, MC 12345",
    siret: String = "12345678901234",
    description: String? = "This is a mock company for testing purposes.",
    siteUrl: String = "https://www.mockcompany.com",
): CompanyEntity = transaction {
    CompanyEntity.new(id) {
        this.name = name
        this.headOffice = headOffice
        this.siret = siret
        this.description = description
        this.siteUrl = siteUrl
        this.logoUrlOriginal = null
        this.logoUrl1000 = null
        this.logoUrl500 = null
        this.logoUrl250 = null
    }
}
