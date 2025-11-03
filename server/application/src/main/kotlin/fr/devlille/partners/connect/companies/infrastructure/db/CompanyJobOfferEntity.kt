package fr.devlille.partners.connect.companies.infrastructure.db

import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import java.util.UUID

/**
 * Exposed entity for company job offers.
 * Provides object-relational mapping for job offer data.
 */
class CompanyJobOfferEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<CompanyJobOfferEntity>(CompanyJobOfferTable) {
        fun singleByCompanyAndJobOffer(companyId: UUID, jobOfferId: UUID): CompanyJobOfferEntity? = this
            .find { (CompanyJobOfferTable.id eq jobOfferId) and (CompanyJobOfferTable.companyId eq companyId) }
            .singleOrNull()
    }

    /** URL to the detailed job posting */
    var url by CompanyJobOfferTable.url

    /** Job position title */
    var title by CompanyJobOfferTable.title

    /** Work location */
    var location by CompanyJobOfferTable.location

    /** Date when the job offer was published */
    var publicationDate by CompanyJobOfferTable.publicationDate

    /** Optional application deadline */
    var endDate by CompanyJobOfferTable.endDate

    /** Optional minimum years of experience required */
    var experienceYears by CompanyJobOfferTable.experienceYears

    /** Optional salary information */
    var salary by CompanyJobOfferTable.salary

    /** Record creation timestamp (read-only) */
    val createdAt by CompanyJobOfferTable.createdAt

    /** Record last modification timestamp */
    var updatedAt by CompanyJobOfferTable.updatedAt

    /**
     * Reference to the company entity.
     * Provides navigation from job offer to company.
     */
    var company by CompanyEntity referencedOn CompanyJobOfferTable.companyId
}
