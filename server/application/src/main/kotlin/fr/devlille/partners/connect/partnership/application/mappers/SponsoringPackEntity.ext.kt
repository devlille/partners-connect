package fr.devlille.partners.connect.partnership.application.mappers

import fr.devlille.partners.connect.partnership.domain.PartnershipPack
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipOptionEntity
import fr.devlille.partners.connect.sponsoring.infrastructure.db.PackOptionsTable
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringPackEntity
import fr.devlille.partners.connect.sponsoring.infrastructure.db.listOptionsByPack
import java.util.UUID

/**
 * Maps SponsoringPackEntity to PartnershipPack with partnership-specific options.
 *
 * Separates options into required and optional lists based on PackOptionsTable,
 * calculates totalPrice as basePrice + sum of optional option amounts.
 *
 * @param language Partnership language for translation lookup
 * @param partnershipId UUID of the partnership
 * @return PartnershipPack with requiredOptions, optionalOptions, and totalPrice
 */
internal fun SponsoringPackEntity.toDomain(
    language: String,
    partnershipId: UUID,
): PartnershipPack {
    // Get all partnership options for this pack
    val partnershipOptions = PartnershipOptionEntity
        .listByPartnershipAndPack(partnershipId, this.id.value)
        .map { it.toDomain(language) }

    // Get pack option configuration to determine required vs optional
    val packOptionRows = PackOptionsTable.listOptionsByPack(this.id.value)
    val requiredOptionIds = packOptionRows
        .filter { it[PackOptionsTable.required] }
        .map { it[PackOptionsTable.option].value.toString() }
        .toSet()

    // Separate into required and optional lists
    val requiredOptions = partnershipOptions.filter { requiredOptionIds.contains(it.id) }
    val optionalOptions = partnershipOptions.filter { !requiredOptionIds.contains(it.id) }

    // Calculate total price: base price + sum of optional option amounts
    val totalPrice = basePrice + optionalOptions.sumOf { it.totalPrice }

    return PartnershipPack(
        id = this.id.value.toString(),
        name = this.name,
        basePrice = this.basePrice,
        requiredOptions = requiredOptions,
        optionalOptions = optionalOptions,
        totalPrice = totalPrice,
    )
}
