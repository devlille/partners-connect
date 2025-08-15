package fr.devlille.partners.connect.organisations.domain

import java.util.UUID

interface OrganisationRepository {
    fun create(entity: Organisation): UUID

    fun getById(id: UUID): Organisation
}
