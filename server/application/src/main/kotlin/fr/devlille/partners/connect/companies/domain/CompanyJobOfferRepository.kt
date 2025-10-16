package fr.devlille.partners.connect.companies.domain

import fr.devlille.partners.connect.internal.infrastructure.api.PaginatedResponse
import java.util.UUID

/**
 * Repository interface for managing job offers associated with companies.
 * Provides CRUD operations and company-specific queries for job offers.
 */
interface CompanyJobOfferRepository {
    /**
     * Creates a new job offer for the specified company.
     *
     * @param companyId UUID of the company creating the job offer
     * @param jobOffer Job offer data to create
     * @return UUID of the created job offer
     */
    suspend fun create(companyId: UUID, jobOffer: CreateJobOffer): UUID

    /**
     * Finds a job offer by its ID.
     *
     * @param jobOfferId UUID of the job offer to find
     * @return JobOfferResponse
     * @throws io.ktor.server.plugins.NotFoundException if job offer is not found
     */
    suspend fun findById(jobOfferId: UUID): JobOfferResponse

    /**
     * Finds all job offers for a specific company with pagination.
     *
     * @param companyId UUID of the company to query job offers for
     * @param page Page number (1-based)
     * @param pageSize Number of items per page
     * @return Paginated result containing job offers and pagination metadata
     */
    suspend fun findByCompany(
        companyId: UUID,
        page: Int = 1,
        pageSize: Int = 20,
    ): PaginatedResponse<JobOfferResponse>

    /**
     * Updates an existing job offer with partial or complete data.
     * Only non-null fields in the UpdateJobOffer will be updated.
     *
     * @param jobOfferId UUID of the job offer to update
     * @param jobOffer Update data containing fields to modify
     * @param companyId UUID of the company that owns the job offer (for ownership validation)
     * @throws io.ktor.server.plugins.NotFoundException if job offer is not found or not owned by company
     */
    suspend fun update(jobOfferId: UUID, jobOffer: UpdateJobOffer, companyId: UUID)

    /**
     * Deletes a job offer by its ID.
     *
     * @param jobOfferId UUID of the job offer to delete
     * @param companyId UUID of the company that owns the job offer (for ownership validation)
     * @throws io.ktor.server.plugins.NotFoundException if job offer is not found or not owned by company
     */
    suspend fun delete(jobOfferId: UUID, companyId: UUID)

    /**
     * Checks if a job offer exists and belongs to the specified company.
     *
     * @param companyId UUID of the company
     * @param jobOfferId UUID of the job offer
     * @return true if the job offer exists and is owned by the company, false otherwise
     */
    suspend fun existsByCompanyAndId(companyId: UUID, jobOfferId: UUID): Boolean
}
