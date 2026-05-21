package fr.devlille.partners.connect.events.application

import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipOptionEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.validatedPack
import fr.devlille.partners.connect.sponsoring.domain.OptionType
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringPackEntity

/**
 * Pricing pack used to value a partnership for the budget endpoint.
 * Order: validated → suggestion → selected. Null when no pack is chosen yet.
 */
fun PartnershipEntity.pricingPack(): SponsoringPackEntity? =
    validatedPack() ?: suggestionPack ?: selectedPack

/**
 * Effective price for a single PartnershipOptionEntity.
 *
 * Mirrors `PartnershipOptionEntity.toDomain(...).totalPrice` in partnership/application/mappers.
 * Defined here as a pure function over the entity so callers can pre-load options in batch
 * and compute totals in memory without re-issuing per-row queries.
 */
@Suppress("CyclomaticComplexMethod")
fun PartnershipOptionEntity.effectivePrice(): Int = when (option.optionType) {
    OptionType.TEXT -> priceOverride ?: option.price ?: 0
    OptionType.TYPED_QUANTITATIVE -> (priceOverride ?: option.price ?: 0) * (selectedQuantity ?: 0)
    OptionType.TYPED_NUMBER -> (priceOverride ?: option.price ?: 0) * (option.fixedQuantity ?: 0)
    OptionType.TYPED_SELECTABLE -> priceOverride
        ?: selectedValue?.price
        ?: error("Selectable option ${option.id.value} has no selected value")
}
