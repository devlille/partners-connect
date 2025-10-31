package fr.devlille.partners.connect.companies.domain

import fr.devlille.partners.connect.internal.infrastructure.api.PaginatedResponse
import java.util.UUID

interface CompanyRepository {
    /**
     * Lists companies with optional search and status filtering.
     *
     * @param query Search query (optional)
     * @param status Filter by company status (optional, defaults to showing all)
     * @param page Page number (1-based)
     * @param pageSize Items per page
     * @return Paginated company results with status information
     */
    fun listPaginated(
        query: String?,
        status: CompanyStatus?,
        page: Int,
        pageSize: Int,
    ): PaginatedResponse<Company>

    fun getById(id: UUID): Company

    fun createOrUpdate(input: CreateCompany): UUID

    fun updateLogoUrls(companyId: UUID, uploaded: Media): UUID

    /**
     * Updates an existing company with partial data.
     * Only non-null fields in UpdateCompany are applied.
     *
     * @param id Company UUID to update
     * @param input Partial update data
     * @return Updated company information
     * @throws NotFoundException if company does not exist
     */
    fun update(id: UUID, input: UpdateCompany): Company

    /**
     * Soft deletes a company by marking it as INACTIVE.
     * Preserves all relationships and data integrity.
     *
     * @param id Company UUID to soft delete
     * @return Company UUID (for consistency with other methods)
     * @throws NotFoundException if company does not exist
     */
    fun softDelete(id: UUID): UUID
}
