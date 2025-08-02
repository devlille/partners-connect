package fr.devlille.partners.connect.partnership.application.mappers

import fr.devlille.partners.connect.partnership.domain.PartnershipPack
import fr.devlille.partners.connect.sponsoring.application.mappers.toDomain
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringPackEntity
import java.util.UUID

internal fun SponsoringPackEntity.toDomain(
    language: String,
    optionIds: List<UUID>,
): PartnershipPack = PartnershipPack(
    id = this.id.value.toString(),
    name = this.name,
    basePrice = this.basePrice,
    options = this.options
        .filter { optionIds.contains(it.id.value) }
        .map { option -> option.toDomain(language) },
)
