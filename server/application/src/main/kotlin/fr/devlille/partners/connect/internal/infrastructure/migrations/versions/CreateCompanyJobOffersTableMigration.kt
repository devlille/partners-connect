package fr.devlille.partners.connect.internal.infrastructure.migrations.versions

import fr.devlille.partners.connect.companies.infrastructure.db.CompanyJobOfferTable
import fr.devlille.partners.connect.internal.infrastructure.migrations.Migration
import org.jetbrains.exposed.v1.jdbc.SchemaUtils

/**
 * Migration to create the company job offers table.
 * Creates the job_offers table with company relationship and full-text search capabilities.
 */
object CreateCompanyJobOffersTableMigration : Migration {
    override val id = "20250128_create_company_job_offers_table"
    override val description = "Create company job offers table with constraints and indexes"

    override fun up() {
        // Create the new job offers table
        SchemaUtils.createMissingTablesAndColumns(CompanyJobOfferTable)
    }

    override fun down() {
        throw UnsupportedOperationException(
            "Rollback not supported for this migration - would require dropping table with potential data loss",
        )
    }
}
