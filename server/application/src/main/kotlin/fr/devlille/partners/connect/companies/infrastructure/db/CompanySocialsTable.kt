package fr.devlille.partners.connect.companies.infrastructure.db

import fr.devlille.partners.connect.companies.domain.SocialType
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable

object CompanySocialsTable : UUIDTable("company_socials") {
    val companyId = reference("company_id", CompaniesTable)
    val type = enumeration<SocialType>("type")
    val url = text("url")
}
