package fr.devlille.partners.connect.organisations.domain

interface OrganisationRepository {
    fun create(entity: Organisation): String

    fun getById(slug: String): Organisation
}
