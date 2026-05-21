package fr.devlille.partners.connect.events.application.mappers

import fr.devlille.partners.connect.events.application.budgetStatus
import fr.devlille.partners.connect.events.application.effectivePrice
import fr.devlille.partners.connect.events.application.pricingPack
import fr.devlille.partners.connect.events.domain.BudgetTotals
import fr.devlille.partners.connect.events.domain.PackBudget
import fr.devlille.partners.connect.events.domain.PartnershipBudgetItem
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipOptionEntity
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringPackEntity
import java.util.UUID

/**
 * Effective price for a partnership using its pricing pack (validated → suggestion → selected).
 * Reads pre-loaded option rows and the pack-required-option map; issues no DB queries.
 * Returns 0 when the partnership has no pricing pack.
 */
internal fun PartnershipEntity.computePriceApplied(
    optionsByPartnership: Map<UUID, List<PartnershipOptionEntity>>,
    requiredOptionIdsByPack: Map<UUID, Set<UUID>>,
): Int {
    val pack = pricingPack() ?: return 0
    val effectiveBase = packPriceOverride ?: pack.basePrice
    val requiredIds = requiredOptionIdsByPack[pack.id.value] ?: emptySet()
    val partnershipOptions = optionsByPartnership[id.value] ?: emptyList()
    val optionalSum = partnershipOptions
        .filter { it.pack.id.value == pack.id.value }
        .filter { it.option.id.value !in requiredIds }
        .sumOf { it.effectivePrice() }
    return effectiveBase + optionalSum
}

internal fun PartnershipEntity.toBudgetItem(
    priceApplied: Int,
    paidPartnershipIds: Set<UUID>,
): PartnershipBudgetItem = PartnershipBudgetItem(
    partnershipId = id.value.toString(),
    companyName = company.name,
    priceApplied = priceApplied,
    status = budgetStatus(paidPartnershipIds),
)

/**
 * Aggregate BudgetTotals across a list of (partnership, price) pairs.
 * Used for both the event-level totals and per-pack totals — the math is identical,
 * only the input set differs.
 */
internal fun List<Pair<PartnershipEntity, Int>>.toBudgetTotals(
    paidPartnershipIds: Set<UUID>,
): BudgetTotals {
    val paid = filter { (p, _) -> p.id.value in paidPartnershipIds }.sumOf { (_, price) -> price }
    val validated = filter { (p, _) -> p.validatedAt != null }.sumOf { (_, price) -> price }
    val total = sumOf { (_, price) -> price }
    return BudgetTotals(
        paid = paid,
        validated = validated,
        validatedMinusPaid = validated - paid,
        total = total,
        totalMinusValidated = total - validated,
    )
}

internal fun SponsoringPackEntity.toBudget(
    partnershipsWithPrice: List<Pair<PartnershipEntity, Int>>,
    paidPartnershipIds: Set<UUID>,
): PackBudget = PackBudget(
    packId = id.value.toString(),
    packName = name,
    basePrice = basePrice,
    totals = partnershipsWithPrice.toBudgetTotals(paidPartnershipIds),
    partnerships = partnershipsWithPrice
        .sortedBy { (p, _) -> p.company.name.lowercase() }
        .map { (p, price) -> p.toBudgetItem(price, paidPartnershipIds) },
)
