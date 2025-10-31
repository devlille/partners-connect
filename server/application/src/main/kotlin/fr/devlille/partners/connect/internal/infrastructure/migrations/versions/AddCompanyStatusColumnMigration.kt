package fr.devlille.partners.connect.internal.infrastructure.migrations.versions

import fr.devlille.partners.connect.companies.infrastructure.db.CompaniesTable
import fr.devlille.partners.connect.internal.infrastructure.migrations.Migration
import org.jetbrains.exposed.v1.jdbc.SchemaUtils

class AddCompanyStatusColumnMigration : Migration {
    override val id: String = "20251030_120000_add_company_status_column"

    override val description: String = "Add status column to companies table with ACTIVE default and index"

    override fun up() {
        SchemaUtils.createMissingTablesAndColumns(CompaniesTable)
    }

    override fun down() {
        // Note: Rolling back enum column requires careful handling in production
        // This migration adds a column with default value, making rollback complex
        // In production, would need manual SQL to drop column and index
    }
}
