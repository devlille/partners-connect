package fr.devlille.partners.connect.ecosystempartners.domain

import java.util.UUID

interface EcosystemPartnerCategoryRepository {
    fun create(eventSlug: String, request: RegisterEcosystemPartnerCategory): UUID

    fun listByEvent(eventSlug: String): List<EcosystemPartnerCategory>

    fun update(eventSlug: String, categoryId: UUID, request: UpdateEcosystemPartnerCategory): EcosystemPartnerCategory

    fun delete(eventSlug: String, categoryId: UUID)
}
