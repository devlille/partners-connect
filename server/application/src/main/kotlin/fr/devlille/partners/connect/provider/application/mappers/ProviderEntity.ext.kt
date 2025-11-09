package fr.devlille.partners.connect.provider.application.mappers

import fr.devlille.partners.connect.provider.domain.Provider
import fr.devlille.partners.connect.provider.infrastructure.db.ProviderEntity

fun ProviderEntity.toDomain(): Provider = Provider(
    id = id.value.toString(),
    name = name,
    type = type,
    website = website,
    phone = phone,
    email = email,
    orgSlug = organisation.slug,
    createdAt = createdAt,
)
