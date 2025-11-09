package fr.devlille.partners.connect.provider.factories

import fr.devlille.partners.connect.organisations.infrastructure.db.OrganisationEntity
import fr.devlille.partners.connect.provider.infrastructure.db.ProviderEntity
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

@Suppress("LongParameterList")
fun insertMockedProvider(
    id: UUID = UUID.randomUUID(),
    name: String = "Test Provider",
    type: String = "Technology",
    website: String? = "https://testprovider.com",
    phone: String? = "+33123456789",
    email: String? = "contact@testprovider.com",
    organisation: OrganisationEntity,
): ProviderEntity = transaction {
    ProviderEntity.new(id) {
        this.name = name
        this.type = type
        this.website = website
        this.phone = phone
        this.email = email
        this.organisation = organisation
    }
}
