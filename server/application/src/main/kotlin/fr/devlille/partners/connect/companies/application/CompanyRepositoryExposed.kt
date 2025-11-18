package fr.devlille.partners.connect.companies.application

import fr.devlille.partners.connect.companies.application.mappers.toDomain
import fr.devlille.partners.connect.companies.domain.Company
import fr.devlille.partners.connect.companies.domain.CompanyRepository
import fr.devlille.partners.connect.companies.domain.CompanyStatus
import fr.devlille.partners.connect.companies.domain.CreateCompany
import fr.devlille.partners.connect.companies.domain.Media
import fr.devlille.partners.connect.companies.domain.UpdateCompany
import fr.devlille.partners.connect.companies.infrastructure.db.CompaniesTable
import fr.devlille.partners.connect.companies.infrastructure.db.CompanyEntity
import fr.devlille.partners.connect.companies.infrastructure.db.CompanySocialEntity
import fr.devlille.partners.connect.internal.infrastructure.api.ConflictException
import fr.devlille.partners.connect.internal.infrastructure.api.PaginatedResponse
import fr.devlille.partners.connect.internal.infrastructure.api.paginated
import fr.devlille.partners.connect.internal.infrastructure.api.toPaginatedResponse
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

class CompanyRepositoryExposed : CompanyRepository {
    override fun listPaginated(
        query: String?,
        status: CompanyStatus?,
        page: Int,
        pageSize: Int,
    ): PaginatedResponse<Company> = transaction {
        val companies = CompanyEntity.listByQueryAndStatus(query, status)
        val total = companies.count()
        companies
            .paginated(page, pageSize)
            .map { companyEntity ->
                companyEntity.toDomain(companyEntity.socials.map(CompanySocialEntity::toDomain))
            }
            .toPaginatedResponse(total, page, pageSize)
    }

    override fun getById(id: UUID): Company = transaction {
        val company = CompanyEntity.findById(id)
        company?.toDomain(company.socials.map(CompanySocialEntity::toDomain))
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

    override fun update(id: UUID, input: UpdateCompany): Company = transaction {
        val company = CompanyEntity.findById(id)
            ?: throw NotFoundException("Company with id $id not found")

        // Check for SIRET conflicts if provided
        input.siret?.let { newSiret ->
            val existingCompany = CompanyEntity
                .find { CompaniesTable.siret eq newSiret }
                .firstOrNull { it.id.value != id }
            if (existingCompany != null) {
                throw ConflictException("Company with SIRET $newSiret already exists")
            }
        }

        // Update only non-null fields (partial update)
        input.name?.let { company.name = it }
        input.siteUrl?.let { company.siteUrl = it }
        input.headOffice?.let { address ->
            company.address = address.address
            company.city = address.city
            company.zipCode = address.zipCode
            company.country = address.country
        }
        input.siret?.let { company.siret = it }
        input.vat?.let { company.vat = it }
        input.description?.let { company.description = it }

        // Update socials if provided
        input.socials?.let { socials ->
            CompanySocialEntity.deleteAllByCompanyId(company.id.value)
            socials.forEach { social ->
                CompanySocialEntity.new {
                    this.company = company
                    this.type = social.type
                    this.url = social.url
                }
            }
        }

        company.toDomain(company.socials.map(CompanySocialEntity::toDomain))
    }

    override fun softDelete(id: UUID): UUID = transaction {
        val company = CompanyEntity.findById(id)
            ?: throw NotFoundException("Company with id $id not found")

        company.status = CompanyStatus.INACTIVE
        company.id.value
    }
}
