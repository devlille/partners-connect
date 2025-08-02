package fr.devlille.partners.connect.companies.application

import fr.devlille.partners.connect.companies.application.mappers.toDomain
import fr.devlille.partners.connect.companies.domain.Company
import fr.devlille.partners.connect.companies.domain.CompanyRepository
import fr.devlille.partners.connect.companies.domain.CreateCompany
import fr.devlille.partners.connect.companies.domain.Media
import fr.devlille.partners.connect.companies.infrastructure.db.CompanyEntity
import fr.devlille.partners.connect.companies.infrastructure.db.CompanySocialEntity
import fr.devlille.partners.connect.companies.infrastructure.db.deleteAllByCompanyId
import fr.devlille.partners.connect.companies.infrastructure.db.listByQuery
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

class CompanyRepositoryExposed(
    private val companyEntity: UUIDEntityClass<CompanyEntity> = CompanyEntity,
    private val companySocialEntity: UUIDEntityClass<CompanySocialEntity> = CompanySocialEntity,
) : CompanyRepository {
    override fun list(query: String?): List<Company> = transaction {
        companyEntity
            .listByQuery(query)
            .map(CompanyEntity::toDomain)
    }

    override fun getById(id: UUID): Company = transaction {
        companyEntity.findById(id)?.toDomain()
            ?: throw NotFoundException("Company with id $id not found")
    }

    override fun createOrUpdate(input: CreateCompany): UUID = transaction {
        val company = companyEntity.new {
            name = input.name
            siteUrl = input.siteUrl
            headOffice = input.headOffice
            siret = input.siret
            description = input.description
        }

        companySocialEntity.deleteAllByCompanyId(company.id.value)

        input.socials.forEach {
            companySocialEntity.new {
                this.company = company
                this.type = it.type
                this.url = it.url
            }
        }

        company.id.value
    }

    override fun updateLogoUrls(companyId: UUID, uploaded: Media): UUID = transaction {
        val company = companyEntity[companyId]
        company.logoUrlOriginal = uploaded.original
        company.logoUrl1000 = uploaded.png1000
        company.logoUrl500 = uploaded.png500
        company.logoUrl250 = uploaded.png250
        company.id.value
    }
}
