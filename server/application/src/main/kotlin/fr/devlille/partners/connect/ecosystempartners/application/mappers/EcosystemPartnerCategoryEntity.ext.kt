package fr.devlille.partners.connect.ecosystempartners.application.mappers

import fr.devlille.partners.connect.ecosystempartners.domain.EcosystemPartnerCategory
import fr.devlille.partners.connect.ecosystempartners.infrastructure.db.EcosystemPartnerCategoryEntity

fun EcosystemPartnerCategoryEntity.toDomain(): EcosystemPartnerCategory = EcosystemPartnerCategory(
    id = id.value.toString(),
    eventId = event.id.value.toString(),
    name = name,
    displayOrder = displayOrder,
)
