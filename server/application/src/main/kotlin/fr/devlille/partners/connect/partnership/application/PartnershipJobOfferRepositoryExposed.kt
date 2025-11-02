package fr.devlille.partners.connect.partnership.application

import fr.devlille.partners.connect.auth.domain.UserInfo
import fr.devlille.partners.connect.companies.domain.JobOfferPromotionResponse
import fr.devlille.partners.connect.companies.infrastructure.db.CompanyJobOfferPromotionEntity
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
import fr.devlille.partners.connect.partnership.domain.PartnershipJobOfferRepository
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEntity
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

/**
 * Exposed-based implementation of PartnershipJobOfferRepository.
 *
 * Handles job offer promotion operations from the organizer/partnership perspective,
 * including viewing promoted job offers and approving/declining promotion requests.
 * All operations must be protected by AuthorizedOrganisationPlugin in route handlers.
 */
class PartnershipJobOfferRepositoryExposed : PartnershipJobOfferRepository {
    override fun listPartnershipJobOffers(
        eventSlug: String,
        partnershipId: UUID,
        status: PromotionStatus?,
        page: Int,
        pageSize: Int,
    ): PaginatedResponse<JobOfferPromotionResponse> = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug '$eventSlug' not found")
        PartnershipEntity.singleByEventAndPartnership(event.id.value, partnershipId)
            ?: throw NotFoundException("Partnership $partnershipId not found for event '$eventSlug'")
        val promotions = CompanyJobOfferPromotionEntity.listByPartnershipAndStatus(partnershipId, status)
        val items = promotions
            .paginated(page, pageSize)
            .map { promotion -> promotion.toDomain() }
        items.toPaginatedResponse(promotions.count(), page, pageSize)
    }

    override fun listEventJobOffers(
        orgSlug: String,
        eventSlug: String,
        status: PromotionStatus?,
        page: Int,
        pageSize: Int,
    ): PaginatedResponse<JobOfferPromotionResponse> = transaction {
        val org = OrganisationEntity.orgFindBySlug(orgSlug)
            ?: throw NotFoundException("Organisation with slug '$orgSlug' not found")
        val event = EventEntity
            .find { (EventsTable.slug eq eventSlug) and (EventsTable.organisationId eq org.id) }
            .singleOrNull()
            ?: throw NotFoundException("Event with slug '$eventSlug' not found for organisation '$orgSlug'")
        val promotions = CompanyJobOfferPromotionEntity.listByEventAndStatus(event.id.value, status)
        val items = promotions
            .paginated(page, pageSize)
            .map { promotion -> promotion.toDomain() }
        items.toPaginatedResponse(promotions.count(), page, pageSize)
    }

    override fun approvePromotion(
        promotionId: UUID,
        reviewer: UserInfo,
    ): JobOfferPromotionResponse = transaction {
        val promotion = CompanyJobOfferPromotionEntity.findById(promotionId)
            ?: throw NotFoundException("Promotion with ID $promotionId not found")

        if (promotion.status != PromotionStatus.PENDING) {
            throw ConflictException(
                "Cannot approve promotion with status ${promotion.status}. Only PENDING promotions can be approved.",
            )
        }

        val reviewer = UserEntity.singleUserByEmail(reviewer.email)
            ?: throw NotFoundException("User not found: ${reviewer.email}")

        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        promotion.status = PromotionStatus.APPROVED
        promotion.reviewedAt = now
        promotion.reviewedBy = reviewer
        promotion.updatedAt = now

        promotion.toDomain()
    }

    override fun declinePromotion(
        promotionId: UUID,
        reviewer: UserInfo,
        reason: String?,
    ): JobOfferPromotionResponse = transaction {
        val promotion = CompanyJobOfferPromotionEntity.findById(promotionId)
            ?: throw NotFoundException("Promotion with ID $promotionId not found")

        if (promotion.status != PromotionStatus.PENDING) {
            throw ConflictException(
                "Cannot decline promotion with status ${promotion.status}. Only PENDING promotions can be declined.",
            )
        }

        val reviewer = UserEntity.singleUserByEmail(reviewer.email)
            ?: throw NotFoundException("User not found: ${reviewer.email}")

        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        promotion.status = PromotionStatus.DECLINED
        promotion.reviewedAt = now
        promotion.reviewedBy = reviewer
        promotion.declineReason = reason
        promotion.updatedAt = now

        promotion.toDomain()
    }
}
