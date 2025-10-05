package fr.devlille.partners.connect.sponsoring.application.mappers

import fr.devlille.partners.connect.sponsoring.domain.SponsoringPackWithTranslations
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringPackEntity
import java.util.UUID

internal fun SponsoringPackEntity.toDomainWithAllTranslations(
    requiredOptionIds: List<UUID>,
    optionalOptions: List<UUID>,
): SponsoringPackWithTranslations {
    // Get all option entities and batch load their translations to avoid N+1 queries
    val requiredOptionEntities = this.options.filter { requiredOptionIds.contains(it.id.value) }
    val optionalOptionEntities = this.options.filter { optionalOptions.contains(it.id.value) }

    return SponsoringPackWithTranslations(
        id = this.id.value.toString(),
        name = this.name,
        basePrice = this.basePrice,
        maxQuantity = this.maxQuantity,
        requiredOptions = requiredOptionEntities.map { option -> option.toDomainWithAllTranslations() },
        optionalOptions = optionalOptionEntities.map { option -> option.toDomainWithAllTranslations() },
    )
}
