package fr.devlille.partners.connect.events.application

import fr.devlille.partners.connect.events.domain.BudgetTotals
import fr.devlille.partners.connect.events.domain.EventBudget
import fr.devlille.partners.connect.events.domain.EventBudgetRepository
import fr.devlille.partners.connect.events.domain.PackBudget
import fr.devlille.partners.connect.events.domain.PartnershipBudgetItem
import fr.devlille.partners.connect.partnership.infrastructure.db.validatedPack
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
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

class EventBudgetRepositoryExposed : EventBudgetRepository {
    @Suppress("LongMethod")
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

        val paidPartnershipIds: Set<UUID> = BillingEntity
            .find {
                (BillingsTable.eventId eq eventId) and
                    (BillingsTable.partnershipId inList partnershipIds) and
                    (BillingsTable.status eq InvoiceStatus.PAID)
            }
            .map { it.partnership.id.value }
            .toSet()

        val optionsByPartnership: Map<UUID, List<PartnershipOptionEntity>> = PartnershipOptionEntity
            .find { PartnershipOptionsTable.partnershipId inList partnershipIds }
            .toList()
            .groupBy { it.partnership.id.value }

        val pricingPackIds = partnerships.mapNotNull { it.pricingPack()?.id?.value }.toSet()
        val requiredOptionIdsByPack: Map<UUID, Set<UUID>> = if (pricingPackIds.isEmpty()) {
            emptyMap()
        } else {
            PackOptionsTable
                .selectAll()
                .where { PackOptionsTable.pack inList pricingPackIds }
                .toList()
                .filter { it[PackOptionsTable.required] }
                .groupBy({ it[PackOptionsTable.pack].value }, { it[PackOptionsTable.option].value })
                .mapValues { (_, v) -> v.toSet() }
        }

        val priceByPartnership: Map<UUID, Int> = partnerships.associate { p ->
            val pack = p.pricingPack()
            val price = if (pack == null) {
                0
            } else {
                val effectiveBase = p.packPriceOverride ?: pack.basePrice
                val requiredIds = requiredOptionIdsByPack[pack.id.value] ?: emptySet()
                val partnershipOptions = optionsByPartnership[p.id.value] ?: emptyList()
                val optionalSum = partnershipOptions
                    .filter { it.pack.id.value == pack.id.value }
                    .filter { it.option.id.value !in requiredIds }
                    .sumOf { it.effectivePrice() }
                effectiveBase + optionalSum
            }
            p.id.value to price
        }

        val paid = partnerships
            .filter { it.id.value in paidPartnershipIds }
            .sumOf { priceByPartnership[it.id.value] ?: 0 }
        val validated = partnerships
            .filter { it.validatedAt != null }
            .sumOf { priceByPartnership[it.id.value] ?: 0 }
        val total = partnerships.sumOf { priceByPartnership[it.id.value] ?: 0 }

        val packBudgets = partnerships
            .mapNotNull { p ->
                val validatedPack = p.validatedPack() ?: return@mapNotNull null
                Triple(validatedPack, p, priceByPartnership[p.id.value] ?: 0)
            }
            .groupBy { (pack, _, _) -> pack.id.value }
            .map { (_, triples) ->
                val pack = triples.first().first
                PackBudget(
                    packId = pack.id.value.toString(),
                    packName = pack.name,
                    basePrice = pack.basePrice,
                    partnerships = triples
                        .sortedBy { (_, partnership, _) -> partnership.company.name.lowercase() }
                        .map { (_, partnership, price) ->
                            PartnershipBudgetItem(
                                partnershipId = partnership.id.value.toString(),
                                companyName = partnership.company.name,
                                priceApplied = price,
                            )
                        },
                )
            }
            .sortedBy { it.packName.lowercase() }

        EventBudget(
            currency = "EUR",
            totals = BudgetTotals(
                paid = paid,
                validated = validated,
                validatedMinusPaid = validated - paid,
                total = total,
                totalMinusValidated = total - validated,
            ),
            packs = packBudgets,
        )
    }
}
