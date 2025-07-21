package fr.devlille.partners.connect.companies.domain

interface CompanyRepository {
    fun list(query: String?): List<Company>

    fun getById(id: String): Company

    fun createOrUpdate(input: CreateCompany): String

    fun updateLogoUrls(companyId: String, uploaded: Media): String
}
