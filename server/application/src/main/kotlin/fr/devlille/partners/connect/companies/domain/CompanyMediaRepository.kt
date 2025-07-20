package fr.devlille.partners.connect.companies.domain

interface CompanyMediaRepository {
    fun upload(companyId: String, media: MediaBinary): Media
}
