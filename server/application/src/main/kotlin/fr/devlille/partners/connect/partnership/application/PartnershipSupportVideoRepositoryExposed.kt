package fr.devlille.partners.connect.partnership.application

import fr.devlille.partners.connect.auth.domain.UserInfo
import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.events.infrastructure.db.EventsTable
import fr.devlille.partners.connect.events.infrastructure.db.findBySlug
import fr.devlille.partners.connect.internal.infrastructure.api.ConflictException
import fr.devlille.partners.connect.internal.infrastructure.api.PaginatedResponse
import fr.devlille.partners.connect.internal.infrastructure.api.paginated
import fr.devlille.partners.connect.internal.infrastructure.api.toPaginatedResponse
import fr.devlille.partners.connect.internal.infrastructure.db.PromotionStatus
import fr.devlille.partners.connect.organisations.infrastructure.db.OrganisationEntity
import fr.devlille.partners.connect.partnership.application.mappers.toDomain
import fr.devlille.partners.connect.partnership.domain.PartnershipSupportVideoRepository
import fr.devlille.partners.connect.partnership.domain.SupportVideoResponse
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipSupportVideoEntity
import fr.devlille.partners.connect.users.infrastructure.db.UserEntity
import fr.devlille.partners.connect.users.infrastructure.db.singleUserByEmail
import io.ktor.server.plugins.NotFoundException
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import fr.devlille.partners.connect.organisations.infrastructure.db.findBySlug as orgFindBySlug

class PartnershipSupportVideoRepositoryExposed : PartnershipSupportVideoRepository {
    override fun listEventSupportVideos(
        orgSlug: String,
        eventSlug: String,
        status: PromotionStatus?,
        page: Int,
        pageSize: Int,
    ): PaginatedResponse<SupportVideoResponse, Unit> = transaction {
        val org = OrganisationEntity.orgFindBySlug(orgSlug)
            ?: throw NotFoundException("Organisation '$orgSlug' not found")
        val event = EventEntity
            .find { (EventsTable.slug eq eventSlug) and (EventsTable.organisationId eq org.id) }
            .singleOrNull()
            ?: throw NotFoundException("Event '$eventSlug' not found for organisation '$orgSlug'")
        val videos = PartnershipSupportVideoEntity.listByEventAndStatus(event.id.value, status)
        val items = videos.paginated(page, pageSize).map { it.toDomain() }
        items.toPaginatedResponse(videos.count(), page, pageSize)
    }

    override fun approve(
        eventSlug: String,
        partnershipId: UUID,
        videoId: UUID,
        reviewer: UserInfo,
    ): SupportVideoResponse = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event '$eventSlug' not found")
        val video = PartnershipSupportVideoEntity.findById(videoId)
            ?: throw NotFoundException("Support video $videoId not found")
        if (video.event.id.value != event.id.value || video.partnership.id.value != partnershipId) {
            throw NotFoundException("Support video $videoId not found for partnership $partnershipId")
        }
        if (video.status != PromotionStatus.PENDING) {
            throw ConflictException(
                "Cannot approve support video with status ${video.status}. Only PENDING videos can be approved.",
            )
        }
        val user = UserEntity.singleUserByEmail(reviewer.email)
            ?: throw NotFoundException("User not found: ${reviewer.email}")
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        video.status = PromotionStatus.APPROVED
        video.reviewedAt = now
        video.reviewedBy = user
        video.updatedAt = now
        video.toDomain()
    }

    @Suppress("LongParameterList")
    override fun decline(
        eventSlug: String,
        partnershipId: UUID,
        videoId: UUID,
        reviewer: UserInfo,
        reason: String?,
    ): SupportVideoResponse = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event '$eventSlug' not found")
        val video = PartnershipSupportVideoEntity.findById(videoId)
            ?: throw NotFoundException("Support video $videoId not found")
        if (video.event.id.value != event.id.value || video.partnership.id.value != partnershipId) {
            throw NotFoundException("Support video $videoId not found for partnership $partnershipId")
        }
        if (video.status != PromotionStatus.PENDING) {
            throw ConflictException(
                "Cannot decline support video with status ${video.status}. Only PENDING videos can be declined.",
            )
        }
        val user = UserEntity.singleUserByEmail(reviewer.email)
            ?: throw NotFoundException("User not found: ${reviewer.email}")
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        video.status = PromotionStatus.DECLINED
        video.reviewedAt = now
        video.reviewedBy = user
        video.declineReason = reason
        video.updatedAt = now
        video.toDomain()
    }
}
