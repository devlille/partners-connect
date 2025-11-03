package fr.devlille.partners.connect.companies.application

import fr.devlille.partners.connect.companies.domain.CompanyJobOfferPromotionRepository
import fr.devlille.partners.connect.companies.domain.JobOffer
import fr.devlille.partners.connect.companies.domain.JobOfferPromotionResponse
import fr.devlille.partners.connect.companies.infrastructure.db.CompanyJobOfferEntity
import fr.devlille.partners.connect.companies.infrastructure.db.CompanyJobOfferPromotionEntity
import fr.devlille.partners.connect.companies.infrastructure.db.CompanyJobOfferPromotionsTable
import fr.devlille.partners.connect.internal.infrastructure.api.ConflictException
import fr.devlille.partners.connect.internal.infrastructure.api.ForbiddenException
import fr.devlille.partners.connect.internal.infrastructure.api.PaginatedResponse
import fr.devlille.partners.connect.internal.infrastructure.api.paginated
import fr.devlille.partners.connect.internal.infrastructure.api.toPaginatedResponse
import fr.devlille.partners.connect.internal.infrastructure.db.PromotionStatus
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEntity
import io.ktor.server.plugins.NotFoundException
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

/**
 * Exposed-based implementation of CompanyJobOfferPromotionRepository.
 *
 * Handles job offer promotion operations from the company perspective, including
 * validation of business rules, upsert logic for declined promotions, and
 * pagination of promotion lists.
 */
class CompanyJobOfferPromotionRepositoryExposed : CompanyJobOfferPromotionRepository {
    override fun promoteJobOffer(
        companyId: UUID,
        partnershipId: UUID,
        jobOfferId: UUID,
    ): UUID = transaction {
        val jobOffer = CompanyJobOfferEntity.singleByCompanyAndJobOffer(companyId, jobOfferId)
            ?: throw NotFoundException("Job offer $jobOfferId not found for company $companyId")
        val partnership = PartnershipEntity.singleByCompanyAndPartnership(companyId, partnershipId)
            ?: throw NotFoundException("Partnership $partnershipId not found for company $companyId")
        val event = partnership.event

        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        if (event.endTime < now) {
            throw ForbiddenException("Cannot promote job offers after event has ended")
        }

        val existingPromotion = CompanyJobOfferPromotionEntity.find {
            (CompanyJobOfferPromotionsTable.jobOfferId eq jobOfferId) and
                (CompanyJobOfferPromotionsTable.partnershipId eq partnershipId)
        }.singleOrNull()

        val statuses = listOf(PromotionStatus.PENDING, PromotionStatus.APPROVED)
        if (existingPromotion != null && existingPromotion.status in statuses) {
            throw ConflictException(
                "Job offer promotion already exists with status: ${existingPromotion.status.name.lowercase()}",
            )
        }

        val promotionEntity = if (existingPromotion != null && existingPromotion.status == PromotionStatus.DECLINED) {
            // Re-promote: Reset to PENDING and clear review metadata
            existingPromotion.apply {
                this.status = PromotionStatus.PENDING
                this.promotedAt = now
                this.reviewedAt = null
                this.reviewedBy = null
                this.declineReason = null
                this.updatedAt = now
            }
        } else {
            // Create new promotion with status PENDING
            CompanyJobOfferPromotionEntity.new {
                this.jobOffer = jobOffer
                this.partnership = partnership
                this.event = event
                this.status = PromotionStatus.PENDING
                this.promotedAt = now
                this.updatedAt = now
            }
        }

        promotionEntity.id.value
    }

    override fun listJobOfferPromotions(
        companyId: UUID,
        jobOfferId: UUID,
        partnershipId: UUID?,
        page: Int,
        pageSize: Int,
    ): PaginatedResponse<JobOfferPromotionResponse> = transaction {
        CompanyJobOfferEntity.singleByCompanyAndJobOffer(companyId, jobOfferId)
            ?: throw NotFoundException("Job offer $jobOfferId not found for company $companyId")
        val baseQuery = CompanyJobOfferPromotionsTable.jobOfferId eq jobOfferId
        val query = if (partnershipId != null) {
            baseQuery and (CompanyJobOfferPromotionsTable.partnershipId eq partnershipId)
        } else {
            baseQuery
        }
        val promotions = CompanyJobOfferPromotionEntity.find { query }
            .orderBy(CompanyJobOfferPromotionsTable.promotedAt to SortOrder.DESC)
        val total = promotions.count()
        promotions
            .paginated(page, pageSize)
            .map { entity ->
                JobOfferPromotionResponse(
                    id = entity.id.value.toString(),
                    jobOfferId = entity.jobOffer.id.value.toString(),
                    partnershipId = entity.partnership.id.value.toString(),
                    eventSlug = entity.event.slug,
                    status = entity.status,
                    promotedAt = entity.promotedAt,
                    reviewedAt = entity.reviewedAt,
                    reviewedBy = entity.reviewedBy?.toString(),
                    declineReason = entity.declineReason,
                    jobOffer = JobOffer(
                        id = entity.jobOffer.id.value.toString(),
                        title = entity.jobOffer.title,
                        url = entity.jobOffer.url,
                    ),
                    createdAt = entity.createdAt,
                    updatedAt = entity.updatedAt,
                )
            }
            .toPaginatedResponse(total, page, pageSize)
    }
}
