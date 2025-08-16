package fr.devlille.partners.connect.organisations.domain

import java.util.UUID

interface OrganisationRepository {
    fun create(entity: Organisation): String

    fun getById(slug: String): Organisation

    fun update(orgSlug: String, data: Organisation): Organisation

    fun findByOrganizerId(userId: UUID): List<Organisation>

    fun findOrganisationListByOrganizerId(userId: UUID): List<OrganisationListResponse>

    fun findOrganisationListByUserEmail(userEmail: String): List<OrganisationListResponse>
}
