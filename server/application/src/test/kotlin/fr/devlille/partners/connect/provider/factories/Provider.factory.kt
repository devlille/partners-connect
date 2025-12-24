package fr.devlille.partners.connect.provider.factories

import fr.devlille.partners.connect.organisations.infrastructure.db.OrganisationEntity
import fr.devlille.partners.connect.provider.infrastructure.db.ProviderEntity
import java.util.UUID

@Suppress("LongParameterList")
fun insertMockedProvider(
    id: UUID = UUID.randomUUID(),
    name: String = id.toString(),
    type: String = "Technology",
    website: String? = "https://testprovider.com",
    phone: String? = "+33123456789",
    email: String? = "$id@testprovider.com",
    orgId: UUID,
): ProviderEntity = ProviderEntity.new(id) {
    this.name = name
    this.type = type
    this.website = website
    this.phone = phone
    this.email = email
    this.organisation = OrganisationEntity[orgId]
}
