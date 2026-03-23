package fr.devlille.partners.connect.partnership.application

import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.events.infrastructure.db.findBySlug
import fr.devlille.partners.connect.partnership.domain.CommunicationItem
import fr.devlille.partners.connect.partnership.domain.CommunicationPlan
import fr.devlille.partners.connect.partnership.domain.PartnershipCommunicationRepository
import fr.devlille.partners.connect.partnership.infrastructure.db.CommunicationPlanEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.CommunicationPlansTable
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipsTable
import io.ktor.server.plugins.NotFoundException
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.isNull
import org.jetbrains.exposed.v1.core.and
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

        val partnershipIdsWithCommunication = communicationItems
            .mapNotNull { it.partnershipId }
            .toSet()

        val unplannedPartnerships = PartnershipEntity
            .find {
                (PartnershipsTable.eventId eq event.id) and
                    PartnershipsTable.declinedAt.isNull()
            }
            .filter { it.id.value.toString() !in partnershipIdsWithCommunication }
            .map { partnership ->
                CommunicationItem(
                    id = partnership.id.value.toString(),
                    partnershipId = partnership.id.value.toString(),
                    title = partnership.company.name,
                    publicationDate = null,
                    supportUrl = null,
                )
            }
            .sortedBy { it.title }

        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        CommunicationPlan(
            done = communicationItems
                .filter { it.publicationDate != null && it.publicationDate < now }
                .sortedByDescending { it.publicationDate },
            planned = communicationItems
                .filter { it.publicationDate != null && it.publicationDate >= now }
                .sortedBy { it.publicationDate },
            unplanned = unplannedPartnerships,
        )
    }
}
