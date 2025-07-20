package fr.devlille.partners.connect.companies.application

import fr.devlille.partners.connect.companies.domain.Company
import fr.devlille.partners.connect.companies.domain.CompanyRepository
import fr.devlille.partners.connect.companies.domain.CreateCompany
import fr.devlille.partners.connect.companies.domain.Media
import fr.devlille.partners.connect.companies.infrastructure.db.CompaniesTable
import fr.devlille.partners.connect.companies.infrastructure.db.CompanyEntity
import fr.devlille.partners.connect.companies.infrastructure.db.CompanySocialEntity
import fr.devlille.partners.connect.companies.infrastructure.db.CompanySocialsTable
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.lowerCase
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.jdbc.andWhere
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

class CompanyRepositoryExposed : CompanyRepository {
    override fun list(query: String?): List<Company> = transaction {
        val filteredQuery = CompaniesTable
            .selectAll()
            .let {
                if (query != null) {
                    val queryLowercase = query.lowercase()
                    it.andWhere {
                        (CompaniesTable.name.lowerCase() like "%$queryLowercase%") or
                            (CompaniesTable.description.lowerCase() like "%${query.lowercase()}%")
                    }
                } else {
                    it
                }
            }
            .orderBy(CompaniesTable.name to SortOrder.ASC)
        CompanyEntity.wrapRows(filteredQuery).map {
            val hasMedia =
                it.logoUrlOriginal != null && it.logoUrl1000 != null && it.logoUrl500 != null && it.logoUrl250 != null
            Company(
                id = it.id.value.toString(),
                name = it.name,
                description = it.description,
                siteUrl = it.siteUrl,
                medias = if (hasMedia) {
                    Media(
                        original = it.logoUrlOriginal!!,
                        png1000 = it.logoUrl1000!!,
                        png500 = it.logoUrl500!!,
                        png250 = it.logoUrl250!!,
                    )
                } else {
                    null
                },
            )
        }
    }

    override fun createOrUpdate(input: CreateCompany): String = transaction {
        val company = CompanyEntity.new {
            name = input.name
            description = input.description
            siteUrl = input.siteUrl
        }

        CompanySocialEntity.find { CompanySocialsTable.companyId eq company.id }.forEach { it.delete() }

        input.socials.forEach {
            CompanySocialEntity.new {
                this.company = company
                this.type = it.type
                this.url = it.url
            }
        }

        company.id.value.toString()
    }

    override fun updateLogoUrls(companyId: String, uploaded: Media) = transaction {
        val company = CompanyEntity[UUID.fromString(companyId)]
        company.logoUrlOriginal = uploaded.original
        company.logoUrl1000 = uploaded.png1000
        company.logoUrl500 = uploaded.png500
        company.logoUrl250 = uploaded.png250
        company.id.value.toString()
    }
}
