package fr.devlille.partners.connect.partnership.application

import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.events.infrastructure.db.findBySlug
import fr.devlille.partners.connect.partnership.domain.CommunicationItem
import fr.devlille.partners.connect.partnership.domain.CommunicationPlan
import fr.devlille.partners.connect.partnership.domain.PartnershipCommunicationRepository
import fr.devlille.partners.connect.partnership.infrastructure.db.CommunicationPlanEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.CommunicationPlansTable
import io.ktor.server.plugins.NotFoundException
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class PartnershipCommunicationRepositoryExposed : PartnershipCommunicationRepository {
    override fun listCommunicationPlan(eventSlug: String): CommunicationPlan = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")

        val communicationItems = CommunicationPlanEntity
            .find { CommunicationPlansTable.eventId eq event.id }
            .map { entry ->
                CommunicationItem(
                    id = entry.id.value.toString(),
                    partnershipId = entry.partnership?.id?.value?.toString(),
                    title = entry.title,
                    publicationDate = entry.scheduledDate,
                    supportUrl = entry.supportUrl,
                )
            }

        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        CommunicationPlan(
            done = communicationItems
                .filter { it.publicationDate != null && it.publicationDate < now }
                .sortedByDescending { it.publicationDate },
            planned = communicationItems
                .filter { it.publicationDate != null && it.publicationDate >= now }
                .sortedBy { it.publicationDate },
            unplanned = communicationItems
                .filter { it.publicationDate == null }
                .sortedBy { it.title },
        )
    }
}
