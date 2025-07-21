package fr.devlille.partners.connect.companies.application

import fr.devlille.partners.connect.companies.application.mappers.toDomain
import fr.devlille.partners.connect.companies.domain.Company
import fr.devlille.partners.connect.companies.domain.CompanyRepository
import fr.devlille.partners.connect.companies.domain.CreateCompany
import fr.devlille.partners.connect.companies.domain.Media
import fr.devlille.partners.connect.companies.infrastructure.db.CompaniesTable
import fr.devlille.partners.connect.companies.infrastructure.db.CompanyEntity
import fr.devlille.partners.connect.companies.infrastructure.db.CompanySocialEntity
import fr.devlille.partners.connect.companies.infrastructure.db.CompanySocialsTable
import io.ktor.server.plugins.NotFoundException
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
        CompanyEntity.wrapRows(filteredQuery).map { it.toDomain() }
    }

    override fun getById(id: String): Company = transaction {
        CompanyEntity.findById(UUID.fromString(id))?.toDomain()
            ?: throw NotFoundException("Company with id $id not found")
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
