package fr.devlille.partners.connect.events.application

import fr.devlille.partners.connect.events.application.mappers.computePriceApplied
import fr.devlille.partners.connect.events.application.mappers.toBudget
import fr.devlille.partners.connect.events.application.mappers.toBudgetTotals
import fr.devlille.partners.connect.events.domain.BudgetTotals
import fr.devlille.partners.connect.events.domain.EventBudget
import fr.devlille.partners.connect.events.domain.EventBudgetRepository
import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.events.infrastructure.db.findBySlug
import fr.devlille.partners.connect.partnership.domain.InvoiceStatus
import fr.devlille.partners.connect.partnership.infrastructure.db.BillingEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.BillingsTable
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipOptionEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipOptionsTable
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipsTable
import fr.devlille.partners.connect.sponsoring.infrastructure.db.PackOptionsTable
import fr.devlille.partners.connect.sponsoring.infrastructure.db.listOptionsByPacks
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

class EventBudgetRepositoryExposed : EventBudgetRepository {
    override fun findByEventSlug(eventSlug: String): EventBudget = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        val eventId = event.id.value

        val partnerships = PartnershipEntity
            .filters(
                eventId = eventId,
                packId = null,
                validated = null,
                suggestion = null,
                agreementGenerated = null,
                agreementSigned = null,
                organiserUserId = null,
                declined = false,
            )
            .orderBy(PartnershipsTable.createdAt to SortOrder.ASC)
            .toList()

        if (partnerships.isEmpty()) {
            return@transaction EventBudget(
                currency = "EUR",
                totals = BudgetTotals(0, 0, 0, 0, 0),
                packs = emptyList(),
            )
        }

        val partnershipIds = partnerships.map { it.id.value }.toSet()
        val paidPartnershipIds = loadPaidPartnershipIds(eventId, partnershipIds)
        val optionsByPartnership = loadOptionsByPartnership(partnershipIds)
        val pricingPackIds = partnerships.mapNotNull { it.pricingPack()?.id?.value }.toSet()
        val requiredOptionIdsByPack = loadRequiredOptionIdsByPack(pricingPackIds)

        val partnershipsWithPrice: List<Pair<PartnershipEntity, Int>> = partnerships.map { p ->
            p to p.computePriceApplied(optionsByPartnership, requiredOptionIdsByPack)
        }

        val packBudgets = partnershipsWithPrice
            .mapNotNull { (p, price) -> p.pricingPack()?.let { Triple(it, p, price) } }
            .groupBy { (pack, _, _) -> pack.id.value }
            .map { (_, triples) ->
                val pack = triples.first().first
                val pairs = triples.map { (_, p, price) -> p to price }
                pack.toBudget(pairs, paidPartnershipIds)
            }
            .sortedBy { it.packName.lowercase() }

        EventBudget(
            currency = "EUR",
            totals = partnershipsWithPrice.toBudgetTotals(paidPartnershipIds),
            packs = packBudgets,
        )
    }

    private fun loadPaidPartnershipIds(eventId: UUID, partnershipIds: Set<UUID>): Set<UUID> = BillingEntity
        .find {
            (BillingsTable.eventId eq eventId) and
                (BillingsTable.partnershipId inList partnershipIds) and
                (BillingsTable.status eq InvoiceStatus.PAID)
        }
        .map { it.partnership.id.value }
        .toSet()

    private fun loadOptionsByPartnership(
        partnershipIds: Set<UUID>,
    ): Map<UUID, List<PartnershipOptionEntity>> = PartnershipOptionEntity
        .find { PartnershipOptionsTable.partnershipId inList partnershipIds }
        .toList()
        .groupBy { it.partnership.id.value }

    private fun loadRequiredOptionIdsByPack(packIds: Set<UUID>): Map<UUID, Set<UUID>> {
        if (packIds.isEmpty()) return emptyMap()
        return PackOptionsTable.listOptionsByPacks(packIds)
            .filter { it[PackOptionsTable.required] }
            .groupBy({ it[PackOptionsTable.pack].value }, { it[PackOptionsTable.option].value })
            .mapValues { (_, v) -> v.toSet() }
    }
}
