package fr.devlille.partners.connect.provider.factories

import fr.devlille.partners.connect.provider.domain.CreateProvider
import fr.devlille.partners.connect.provider.infrastructure.db.ProviderEntity
import java.util.UUID

fun createMockedProviderInput(
    name: String = "Test Provider",
    type: String = "Technology",
    website: String? = "https://testprovider.com",
    phone: String? = "+33123456789",
    email: String? = "contact@testprovider.com",
): CreateProvider = CreateProvider(
    name = name,
    type = type,
    website = website,
    phone = phone,
    email = email,
)

fun insertMockedProvider(
    id: UUID = UUID.randomUUID(),
    name: String = "Test Provider",
    type: String = "Technology",
    website: String? = "https://testprovider.com",
    phone: String? = "+33123456789",
    email: String? = "contact@testprovider.com",
): ProviderEntity = ProviderEntity.new(id) {
    this.name = name
    this.type = type
    this.website = website
    this.phone = phone
    this.email = email
}