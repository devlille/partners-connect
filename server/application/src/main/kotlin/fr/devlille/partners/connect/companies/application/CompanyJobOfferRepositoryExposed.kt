package fr.devlille.partners.connect.companies.application

import fr.devlille.partners.connect.companies.domain.CompanyJobOfferRepository
import fr.devlille.partners.connect.companies.domain.CreateJobOffer
import fr.devlille.partners.connect.companies.domain.JobOfferResponse
import fr.devlille.partners.connect.companies.domain.UpdateJobOffer
import fr.devlille.partners.connect.companies.infrastructure.db.CompanyEntity
import fr.devlille.partners.connect.companies.infrastructure.db.CompanyJobOfferEntity
import fr.devlille.partners.connect.companies.infrastructure.db.CompanyJobOfferPromotionEntity
import fr.devlille.partners.connect.companies.infrastructure.db.CompanyJobOfferPromotionsTable
import fr.devlille.partners.connect.companies.infrastructure.db.CompanyJobOfferTable
import fr.devlille.partners.connect.internal.infrastructure.api.PaginatedResponse
import fr.devlille.partners.connect.internal.infrastructure.api.ValidationException
import fr.devlille.partners.connect.internal.infrastructure.api.paginated
import fr.devlille.partners.connect.internal.infrastructure.api.toPaginatedResponse
import io.ktor.server.plugins.NotFoundException
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

/**
 * Exposed-based implementation of CompanyJobOfferRepository.
 * Provides CRUD operations for job offers using Exposed ORM.
 */
class CompanyJobOfferRepositoryExposed : CompanyJobOfferRepository {
    override suspend fun create(companyId: UUID, jobOffer: CreateJobOffer): UUID = transaction {
        validatePublicationDate(jobOffer.publicationDate)

        // Verify company exists first
        val company = CompanyEntity.findById(companyId)
            ?: throw NotFoundException("Company not found: $companyId")

        val entity = CompanyJobOfferEntity.new {
            this.company = company
            this.url = jobOffer.url
            this.title = jobOffer.title
            this.location = jobOffer.location
            this.publicationDate = jobOffer.publicationDate
            this.endDate = jobOffer.endDate
            this.experienceYears = jobOffer.experienceYears ?: 0
            this.salary = jobOffer.salary
            this.updatedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        }

        entity.id.value
    }

    override suspend fun findById(jobOfferId: UUID): JobOfferResponse = transaction {
        val entity = CompanyJobOfferEntity.findById(jobOfferId)
            ?: throw NotFoundException("Job offer not found: $jobOfferId")

        JobOfferResponse(
            id = entity.id.value.toString(),
            companyId = entity.company.id.value.toString(),
            url = entity.url,
            title = entity.title,
            location = entity.location,
            publicationDate = entity.publicationDate.toString(),
            endDate = entity.endDate?.toString(),
            experienceYears = entity.experienceYears,
            salary = entity.salary,
            createdAt = entity.createdAt.toString(),
            updatedAt = entity.updatedAt.toString(),
        )
    }

    override suspend fun findByCompany(
        companyId: UUID,
        page: Int,
        pageSize: Int,
    ): PaginatedResponse<JobOfferResponse> = transaction {
        // Verify company exists
        CompanyEntity.findById(companyId)
            ?: throw NotFoundException("Company not found: $companyId")

        val query = CompanyJobOfferEntity.find {
            CompanyJobOfferTable.companyId eq companyId
        }.orderBy(CompanyJobOfferTable.createdAt to SortOrder.DESC)

        val total = query.count()

        query
            .paginated(page, pageSize)
            .map { entity ->
                JobOfferResponse(
                    id = entity.id.value.toString(),
                    companyId = entity.company.id.value.toString(),
                    url = entity.url,
                    title = entity.title,
                    location = entity.location,
                    publicationDate = entity.publicationDate.toString(),
                    endDate = entity.endDate?.toString(),
                    experienceYears = entity.experienceYears,
                    salary = entity.salary,
                    createdAt = entity.createdAt.toString(),
                    updatedAt = entity.updatedAt.toString(),
                )
            }
            .toPaginatedResponse(total, page, pageSize)
    }

    override suspend fun update(
        jobOfferId: UUID,
        jobOffer: UpdateJobOffer,
        companyId: UUID,
    ) = transaction {
        jobOffer.publicationDate?.let { validatePublicationDate(jobOffer.publicationDate) }

        val entity = CompanyJobOfferEntity.find {
            CompanyJobOfferTable.id eq jobOfferId and (CompanyJobOfferTable.companyId eq companyId)
        }.singleOrNull() ?: throw NotFoundException("Job offer not found or not owned by company: $jobOfferId")

        // Apply partial updates
        jobOffer.url?.let { entity.url = it }
        jobOffer.title?.let { entity.title = it }
        jobOffer.location?.let { entity.location = it }
        jobOffer.publicationDate?.let {
            entity.publicationDate = it
        }
        jobOffer.endDate?.let {
            entity.endDate = it
        }
        jobOffer.experienceYears?.let { entity.experienceYears = it }
        jobOffer.salary?.let { entity.salary = it }

        entity.updatedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC)
    }

    override suspend fun delete(jobOfferId: UUID, companyId: UUID) = transaction {
        CompanyJobOfferPromotionEntity
            .find { CompanyJobOfferPromotionsTable.jobOfferId eq jobOfferId }
            .forEach { it.delete() }

        val entity = CompanyJobOfferEntity.find {
            CompanyJobOfferTable.id eq jobOfferId and (CompanyJobOfferTable.companyId eq companyId)
        }.singleOrNull() ?: throw NotFoundException("Job offer not found or not owned by company: $jobOfferId")

        entity.delete()
    }

    override suspend fun existsByCompanyAndId(companyId: UUID, jobOfferId: UUID): Boolean = transaction {
        !CompanyJobOfferEntity.find {
            CompanyJobOfferTable.id eq jobOfferId and (CompanyJobOfferTable.companyId eq companyId)
        }.empty()
    }

    /**
     * Validates publication date is not in the future.
     * @param publicationDate ISO format publication date
     * @throws ValidationException if date is in the future
     */
    private fun validatePublicationDate(publicationDate: LocalDateTime) {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        if (publicationDate > now) {
            throw ValidationException("publication_date", "cannot be in the future")
        }
    }
}
