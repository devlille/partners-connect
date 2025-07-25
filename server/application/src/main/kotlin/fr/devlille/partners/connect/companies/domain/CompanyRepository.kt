package fr.devlille.partners.connect.companies.domain

import java.util.UUID

interface CompanyRepository {
    fun list(query: String?): List<Company>

    fun getById(id: UUID): Company

    fun createOrUpdate(input: CreateCompany): UUID

    fun updateLogoUrls(companyId: UUID, uploaded: Media): UUID
}
