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
import fr.devlille.partners.connect.internal.infrastructure.api.PaginatedResponse
import fr.devlille.partners.connect.internal.infrastructure.api.paginated
import fr.devlille.partners.connect.internal.infrastructure.api.toPaginatedResponse
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

class CompanyRepositoryExposed : CompanyRepository {
    override fun listPaginated(query: String?, page: Int, pageSize: Int): PaginatedResponse<Company> = transaction {
        val sized = CompanyEntity.listByQuery(query)
        val total = sized.count()
        sized
            .paginated(page, pageSize)
            .map(CompanyEntity::toDomain)
            .toPaginatedResponse(total, page, pageSize)
    }

    override fun getById(id: UUID): Company = transaction {
        CompanyEntity.findById(id)?.toDomain()
            ?: throw NotFoundException("Company with id $id not found")
    }

    override fun createOrUpdate(input: CreateCompany): UUID = transaction {
        val company = CompanyEntity.new {
            name = input.name
            siteUrl = input.siteUrl
            address = input.headOffice.address
            city = input.headOffice.city
            zipCode = input.headOffice.zipCode
            country = input.headOffice.country
            siret = input.siret
            vat = input.vat
            description = input.description
        }

        CompanySocialEntity.deleteAllByCompanyId(company.id.value)

        input.socials.forEach {
            CompanySocialEntity.new {
                this.company = company
                this.type = it.type
                this.url = it.url
            }
        }

        company.id.value
    }

    override fun updateLogoUrls(companyId: UUID, uploaded: Media): UUID = transaction {
        val company = CompanyEntity[companyId]
        company.logoUrlOriginal = uploaded.original
        company.logoUrl1000 = uploaded.png1000
        company.logoUrl500 = uploaded.png500
        company.logoUrl250 = uploaded.png250
        company.id.value
    }
}
