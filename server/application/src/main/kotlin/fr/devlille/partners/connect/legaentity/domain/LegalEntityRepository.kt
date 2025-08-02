package fr.devlille.partners.connect.legaentity.domain

import java.util.UUID

interface LegalEntityRepository {
    fun create(entity: LegalEntity): UUID

    fun getById(id: UUID): LegalEntity
}
