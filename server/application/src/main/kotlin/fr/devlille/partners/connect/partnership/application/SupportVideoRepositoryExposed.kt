package fr.devlille.partners.connect.partnership.application

import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.events.infrastructure.db.findBySlug
import fr.devlille.partners.connect.internal.infrastructure.api.ConflictException
import fr.devlille.partners.connect.internal.infrastructure.db.PromotionStatus
import fr.devlille.partners.connect.partnership.application.mappers.toDomain
import fr.devlille.partners.connect.partnership.domain.SupportVideoRepository
import fr.devlille.partners.connect.partnership.domain.SupportVideoResponse
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipSupportVideoEntity
import io.ktor.server.plugins.NotFoundException
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

class SupportVideoRepositoryExposed : SupportVideoRepository {
    override fun submit(eventSlug: String, partnershipId: UUID, url: String): UUID = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event '$eventSlug' not found")
        val partnership = PartnershipEntity.singleByEventAndPartnership(event.id.value, partnershipId)
            ?: throw NotFoundException("Partnership $partnershipId not found for event '$eventSlug'")

        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val existing = PartnershipSupportVideoEntity.singleByPartnership(partnershipId)
        val entity = when (existing?.status) {
            null -> PartnershipSupportVideoEntity.new {
                this.partnership = partnership
                this.event = event
                this.url = url
                this.status = PromotionStatus.PENDING
                this.submittedAt = now
                this.updatedAt = now
            }
            PromotionStatus.APPROVED -> throw ConflictException(
                "Support video already approved. Contact organisers to revoke before re-submitting.",
            )
            PromotionStatus.PENDING, PromotionStatus.DECLINED -> existing.apply {
                this.url = url
                this.status = PromotionStatus.PENDING
                this.submittedAt = now
                this.reviewedAt = null
                this.reviewedBy = null
                this.declineReason = null
                this.updatedAt = now
            }
        }
        entity.id.value
    }

    override fun get(eventSlug: String, partnershipId: UUID): SupportVideoResponse = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event '$eventSlug' not found")
        PartnershipEntity.singleByEventAndPartnership(event.id.value, partnershipId)
            ?: throw NotFoundException("Partnership $partnershipId not found for event '$eventSlug'")
        val entity = PartnershipSupportVideoEntity.singleByPartnership(partnershipId)
            ?: throw NotFoundException("No support video submitted for partnership $partnershipId")
        entity.toDomain()
    }
}
