package fr.devlille.partners.connect.companies.factories

import fr.devlille.partners.connect.companies.domain.SocialType
import fr.devlille.partners.connect.companies.infrastructure.db.CompanyEntity
import fr.devlille.partners.connect.companies.infrastructure.db.CompanySocialEntity
import java.util.UUID

fun insertMockedCompanySocial(
    id: UUID = UUID.randomUUID(),
    companyId: UUID,
    type: SocialType = SocialType.LINKEDIN,
    url: String = "https://example.com/$id",
): CompanySocialEntity = CompanySocialEntity.new(id) {
    this.company = CompanyEntity[companyId]
    this.type = type
    this.url = url
}
