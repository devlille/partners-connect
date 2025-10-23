package fr.devlille.partners.connect.internal.infrastructure.migrations.versions

import fr.devlille.partners.connect.companies.infrastructure.db.CompanyJobOfferPromotionsTable
import fr.devlille.partners.connect.internal.infrastructure.migrations.Migration
import org.jetbrains.exposed.v1.jdbc.SchemaUtils

/**
 * Migration to create the company job offer promotions table.
 * Creates the company_job_offer_promotions table to track job offer promotions to partnerships.
 */
object CreateJobOfferPromotionsTableMigration : Migration {
    override val id = "20250129_create_job_offer_promotions_table"
    override val description = "Create company job offer promotions table with constraints and indexes"

    override fun up() {
        // Create the new job offer promotions table
        SchemaUtils.createMissingTablesAndColumns(CompanyJobOfferPromotionsTable)
    }

    override fun down() {
        throw UnsupportedOperationException(
            "Rollback not supported for this migration - would require dropping table with potential data loss",
        )
    }
}
