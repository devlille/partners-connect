package fr.devlille.partners.connect.partnership.application

import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.events.infrastructure.db.findBySlug
import fr.devlille.partners.connect.partnership.domain.PartnershipDecisionRepository
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEntity
import io.ktor.server.plugins.NotFoundException
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

class PartnershipDecisionRepositoryExposed : PartnershipDecisionRepository {
    override fun validate(eventSlug: String, partnershipId: UUID): UUID = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        val partnership = PartnershipEntity
            .singleByEventAndPartnership(event.id.value, partnershipId)
            ?: throw NotFoundException("Partnership not found")
        partnership.validatedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        partnership.id.value
    }

    override fun decline(eventSlug: String, partnershipId: UUID): UUID = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        val partnership = PartnershipEntity
            .singleByEventAndPartnership(event.id.value, partnershipId)
            ?: throw NotFoundException("Partnership not found")
        partnership.declinedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        partnership.id.value
    }
}
