package fr.devlille.partners.connect.partnership.application

import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.events.infrastructure.db.findBySlug
import fr.devlille.partners.connect.partnership.domain.CommunicationItem
import fr.devlille.partners.connect.partnership.domain.CommunicationPlan
import fr.devlille.partners.connect.partnership.domain.PartnershipCommunicationRepository
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipsTable
import io.ktor.server.plugins.NotFoundException
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

class PartnershipCommunicationRepositoryExposed : PartnershipCommunicationRepository {
    override fun updateCommunicationPublicationDate(
        eventSlug: String,
        partnershipId: UUID,
        publicationDate: LocalDateTime,
    ): UUID = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        val partnership = PartnershipEntity
            .singleByEventAndPartnership(event.id.value, partnershipId)
            ?: throw NotFoundException("Partnership not found")
        partnership.communicationPublicationDate = publicationDate
        partnership.id.value
    }

    override fun updateCommunicationSupportUrl(
        eventSlug: String,
        partnershipId: UUID,
        supportUrl: String,
    ): UUID = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        val partnership = PartnershipEntity
            .singleByEventAndPartnership(event.id.value, partnershipId)
            ?: throw NotFoundException("Partnership not found")
        partnership.communicationSupportUrl = supportUrl
        partnership.id.value
    }

    override fun listCommunicationPlan(eventSlug: String): CommunicationPlan = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")

        val eventId = event.id.value
        val partnerships = PartnershipEntity.find { PartnershipsTable.eventId eq eventId }

        val communicationItems = partnerships.map { partnership ->
            CommunicationItem(
                partnershipId = partnership.id.value.toString(),
                companyName = partnership.company.name,
                publicationDate = partnership.communicationPublicationDate,
                supportUrl = partnership.communicationSupportUrl,
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
                .sortedBy { it.companyName },
        )
    }
}
