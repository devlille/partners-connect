package fr.devlille.partners.connect.events.application

import fr.devlille.partners.connect.events.domain.PartnershipBudgetStatus
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipOptionEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.validatedPack
import fr.devlille.partners.connect.sponsoring.domain.OptionType
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringPackEntity
import java.util.UUID

/**
 * Pricing pack used to value a partnership for the budget endpoint.
 * Order: validated → suggestion → selected. Null when no pack is chosen yet.
 */
fun PartnershipEntity.pricingPack(): SponsoringPackEntity? =
    validatedPack() ?: suggestionPack ?: selectedPack

/**
 * Budget lifecycle status for a non-declined partnership.
 * Precedence: PAID (billing row PAID) > VALIDATED (validatedAt set) > SUBMITTED (default).
 */
fun PartnershipEntity.budgetStatus(paidPartnershipIds: Set<UUID>): PartnershipBudgetStatus = when {
    id.value in paidPartnershipIds -> PartnershipBudgetStatus.PAID
    validatedAt != null -> PartnershipBudgetStatus.VALIDATED
    else -> PartnershipBudgetStatus.SUBMITTED
}

/**
 * Effective price for a single PartnershipOptionEntity.
 *
 * Mirrors `PartnershipOptionEntity.toDomain(...).totalPrice` in partnership/application/mappers,
 * with one deliberate divergence: a `TYPED_SELECTABLE` option whose `selectedValue` is null
 * contributes `0` rather than throwing. The mapper throws because it's called per-partnership
 * during document/invoice generation, where a corrupt row should abort the operation. The budget
 * endpoint aggregates every non-declined partnership of an event, so a single bad row would break
 * the whole view; returning `0` keeps the rest of the budget visible at the cost of slightly
 * undercounting one line item. Data integrity for selectable options is enforced at write time
 * (`PartnershipOptionEntity.create`), so this fallback is only reached on legacy or corrupt rows.
 */
@Suppress("CyclomaticComplexMethod")
fun PartnershipOptionEntity.effectivePrice(): Int = when (option.optionType) {
    OptionType.TEXT -> priceOverride ?: option.price ?: 0
    OptionType.TYPED_QUANTITATIVE -> (priceOverride ?: option.price ?: 0) * (selectedQuantity ?: 0)
    OptionType.TYPED_NUMBER -> (priceOverride ?: option.price ?: 0) * (option.fixedQuantity ?: 0)
    OptionType.TYPED_SELECTABLE -> priceOverride ?: selectedValue?.price ?: 0
}
