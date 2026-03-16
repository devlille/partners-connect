package fr.devlille.partners.connect.partnership.application

import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.events.infrastructure.db.findBySlug
import fr.devlille.partners.connect.partnership.domain.CommunicationPlanEntry
import fr.devlille.partners.connect.partnership.domain.CommunicationPlanRepository
import fr.devlille.partners.connect.partnership.infrastructure.db.CommunicationPlanEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.CommunicationPlansTable
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEntity
import io.ktor.server.plugins.NotFoundException
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

class CommunicationPlanRepositoryExposed : CommunicationPlanRepository {
    override fun create(
        eventSlug: String,
        title: String,
        scheduledDate: LocalDateTime?,
        description: String?,
        supportUrl: String?,
    ): CommunicationPlanEntry = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        CommunicationPlanEntity.new(UUID.randomUUID()) {
            this.event = event
            this.partnership = null
            this.title = title
            this.scheduledDate = scheduledDate
            this.description = description
            this.supportUrl = supportUrl
        }.toDomain()
    }

    override fun findById(eventSlug: String, id: UUID): CommunicationPlanEntry = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        CommunicationPlanEntity.find {
            (CommunicationPlansTable.id eq id) and (CommunicationPlansTable.eventId eq event.id)
        }.firstOrNull()?.toDomain()
            ?: throw NotFoundException("Communication plan entry $id not found for event $eventSlug")
    }

    override fun update(
        eventSlug: String,
        id: UUID,
        title: String,
        scheduledDate: LocalDateTime?,
        description: String?,
        supportUrl: String?,
    ): CommunicationPlanEntry = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        val entry = CommunicationPlanEntity.find {
            (CommunicationPlansTable.id eq id) and (CommunicationPlansTable.eventId eq event.id)
        }.firstOrNull()
            ?: throw NotFoundException("Communication plan entry $id not found for event $eventSlug")
        entry.title = title
        entry.scheduledDate = scheduledDate
        entry.description = description
        entry.supportUrl = supportUrl
        entry.updatedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        entry.toDomain()
    }

    override fun delete(eventSlug: String, id: UUID) = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        val entry = CommunicationPlanEntity.find {
            (CommunicationPlansTable.id eq id) and (CommunicationPlansTable.eventId eq event.id)
        }.firstOrNull()
            ?: throw NotFoundException("Communication plan entry $id not found for event $eventSlug")
        entry.delete()
    }

    override fun upsertForPartnership(
        eventSlug: String,
        partnershipId: UUID,
        scheduledDate: LocalDateTime?,
        supportUrl: String?,
    ): CommunicationPlanEntry = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")
        val partnership = PartnershipEntity.findById(partnershipId)
            ?: throw NotFoundException("Partnership $partnershipId not found")

        val existing = CommunicationPlanEntity.find {
            CommunicationPlansTable.partnershipId eq partnershipId
        }.firstOrNull()

        if (existing != null) {
            if (scheduledDate != null) existing.scheduledDate = scheduledDate
            if (supportUrl != null) existing.supportUrl = supportUrl
            existing.updatedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC)
            existing.toDomain()
        } else {
            CommunicationPlanEntity.new(UUID.randomUUID()) {
                this.event = event
                this.partnership = partnership
                this.title = partnership.company.name
                this.scheduledDate = scheduledDate
                this.supportUrl = supportUrl
            }.toDomain()
        }
    }

    private fun CommunicationPlanEntity.toDomain(): CommunicationPlanEntry = CommunicationPlanEntry(
        id = id.value.toString(),
        eventId = event.id.value.toString(),
        partnershipId = partnership?.id?.value?.toString(),
        title = title,
        scheduledDate = scheduledDate,
        description = description,
        supportUrl = supportUrl,
        createdAt = createdAt,
    )
}
