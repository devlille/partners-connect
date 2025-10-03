package fr.devlille.partners.connect.sponsoring.application.mappers

import fr.devlille.partners.connect.sponsoring.domain.SponsoringPack
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringPackEntity
import java.util.UUID

internal fun SponsoringPackEntity.toDomain(
    language: String,
    requiredOptionIds: List<UUID>,
    optionalOptions: List<UUID>,
): SponsoringPack = SponsoringPack(
    id = this.id.value.toString(),
    name = this.name,
    basePrice = this.basePrice,
    maxQuantity = this.maxQuantity,
    requiredOptions = this.options
        .filter { requiredOptionIds.contains(it.id.value) }
        .map { option -> option.toDomain(language, required = true) },
    optionalOptions = this.options
        .filter { optionalOptions.contains(it.id.value) }
        .map { option -> option.toDomain(language, required = false) },
)
