package fr.devlille.partners.connect.companies.infrastructure.db

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.datetime

private const val MAX_SALARY_LENGTH = 100

/**
 * Exposed table definition for company job offers.
 * Stores job offer information with foreign key relationship to companies.
 */
object CompanyJobOfferTable : UUIDTable("company_job_offers") {
    /** Foreign key reference to the companies table */
    val companyId = reference("company_id", CompaniesTable.id)

    /** URL link to the job offer */
    val url = text("url")

    /** Job title */
    val title = text("title")

    /** Job location */
    val location = varchar("location", length = 200)

    /** Publication date of the job offer (ISO format: YYYY-MM-DD) */
    val publicationDate = datetime("publication_date")

    /** Optional end date for applications (ISO format: YYYY-MM-DD) */
    val endDate = datetime("end_date").nullable()

    /** Required years of experience */
    val experienceYears = integer("experience_years")

    /** Salary information (optional) */
    val salary = varchar("salary", MAX_SALARY_LENGTH).nullable()

    /** Creation timestamp */
    val createdAt = datetime("created_at").clientDefault {
        Clock.System.now().toLocalDateTime(TimeZone.UTC)
    }

    /** Last update timestamp */
    val updatedAt = datetime("updated_at").clientDefault {
        Clock.System.now().toLocalDateTime(TimeZone.UTC)
    }

    init {
        // Create indexes for performance optimization

        // Index on company_id for efficient company-specific queries
        index(false, companyId)

        // Composite index on company_id and created_at for efficient pagination
        // This supports ORDER BY created_at DESC in findByCompany queries
        index(false, companyId, createdAt)

        // Index on publication_date for date-based filtering and ordering
        index(false, publicationDate)

        // Unique constraint ensuring job offers belong to existing companies
        // (foreign key constraint is already handled by reference() but this improves query planning)
    }
}
