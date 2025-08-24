package fr.devlille.partners.connect.companies.domain

import fr.devlille.partners.connect.internal.infrastructure.api.PaginatedResponse
import java.util.UUID

interface CompanyRepository {
    fun listPaginated(query: String?, page: Int, pageSize: Int): PaginatedResponse<Company>

    fun getById(id: UUID): Company

    fun createOrUpdate(input: CreateCompany): UUID

    fun updateLogoUrls(companyId: UUID, uploaded: Media): UUID
}
