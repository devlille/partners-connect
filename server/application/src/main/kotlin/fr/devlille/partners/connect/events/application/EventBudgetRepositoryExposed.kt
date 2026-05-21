package fr.devlille.partners.connect.events.application

import fr.devlille.partners.connect.events.domain.BudgetTotals
import fr.devlille.partners.connect.events.domain.EventBudget
import fr.devlille.partners.connect.events.domain.EventBudgetRepository
import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.events.infrastructure.db.findBySlug
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class EventBudgetRepositoryExposed : EventBudgetRepository {
    override fun findByEventSlug(eventSlug: String): EventBudget = transaction {
        EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")

        EventBudget(
            currency = "EUR",
            totals = BudgetTotals(
                paid = 0,
                validated = 0,
                validatedMinusPaid = 0,
                total = 0,
                totalMinusValidated = 0,
            ),
            packs = emptyList(),
        )
    }
}
