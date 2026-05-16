package fr.devlille.partners.connect.ecosystempartners.factories

import fr.devlille.partners.connect.companies.infrastructure.db.CompanyEntity
import fr.devlille.partners.connect.ecosystempartners.infrastructure.db.EcosystemPartnerCategoryEntity
import fr.devlille.partners.connect.ecosystempartners.infrastructure.db.EcosystemPartnerEntity
import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import kotlinx.datetime.LocalDateTime
import java.util.UUID

@Suppress("LongParameterList")
fun insertMockedEcosystemPartner(
    id: UUID = UUID.randomUUID(),
    eventId: UUID,
    companyId: UUID,
    categoryId: UUID,
    displayOrder: Int? = null,
    language: String = "en",
    validatedAt: LocalDateTime? = null,
    declinedAt: LocalDateTime? = null,
): EcosystemPartnerEntity {
    val event = EventEntity.findById(eventId)
        ?: error("Event $eventId must exist before creating an ecosystem partner")
    val company = CompanyEntity.findById(companyId)
        ?: error("Company $companyId must exist before creating an ecosystem partner")
    val category = EcosystemPartnerCategoryEntity.findById(categoryId)
        ?: error("Category $categoryId must exist before creating an ecosystem partner")
    return EcosystemPartnerEntity.new(id) {
        this.event = event
        this.company = company
        this.category = category
        this.displayOrder = displayOrder
        this.language = language
        this.validatedAt = validatedAt
        this.declinedAt = declinedAt
    }
}
