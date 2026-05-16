package fr.devlille.partners.connect.ecosystempartners.application

import fr.devlille.partners.connect.ecosystempartners.domain.EcosystemPartnerDecisionRepository
import fr.devlille.partners.connect.ecosystempartners.infrastructure.db.EcosystemPartnerEntity
import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.events.infrastructure.db.findBySlug
import io.ktor.server.plugins.NotFoundException
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

class EcosystemPartnerDecisionRepositoryExposed : EcosystemPartnerDecisionRepository {
    override fun validate(eventSlug: String, ecosystemPartnerId: UUID): UUID = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        val entity = EcosystemPartnerEntity.findById(ecosystemPartnerId)
            ?: throw NotFoundException("Ecosystem partner $ecosystemPartnerId not found")
        if (entity.event.id.value != event.id.value) {
            throw NotFoundException("Ecosystem partner $ecosystemPartnerId does not belong to event $eventSlug")
        }
        entity.validatedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        entity.declinedAt = null
        entity.id.value
    }

    override fun decline(eventSlug: String, ecosystemPartnerId: UUID): UUID = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        val entity = EcosystemPartnerEntity.findById(ecosystemPartnerId)
            ?: throw NotFoundException("Ecosystem partner $ecosystemPartnerId not found")
        if (entity.event.id.value != event.id.value) {
            throw NotFoundException("Ecosystem partner $ecosystemPartnerId does not belong to event $eventSlug")
        }
        entity.declinedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        entity.validatedAt = null
        entity.id.value
    }
}
