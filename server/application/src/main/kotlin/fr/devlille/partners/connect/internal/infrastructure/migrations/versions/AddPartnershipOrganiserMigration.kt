package fr.devlille.partners.connect.internal.infrastructure.migrations.versions

import fr.devlille.partners.connect.internal.infrastructure.migrations.Migration
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipsTable
import org.jetbrains.exposed.v1.jdbc.SchemaUtils

/**
 * Migration to add organiser assignment capability to partnerships.
 * Adds nullable organiser_id foreign key to partnerships table.
 */
object AddPartnershipOrganiserMigration : Migration {
    override val id = "20251122_add_partnership_organiser"
    override val description = "Add organiser_id nullable column to partnerships table"

    override fun up() {
        // Add missing columns to PartnershipsTable - this will add the new organiserId column
        SchemaUtils.createMissingTablesAndColumns(PartnershipsTable)
    }

    override fun down() {
        throw UnsupportedOperationException(
            "Rollback not supported - would require dropping column which could cause data loss",
        )
    }
}
