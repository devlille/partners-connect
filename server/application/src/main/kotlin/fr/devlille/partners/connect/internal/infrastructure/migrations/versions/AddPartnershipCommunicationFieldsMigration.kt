package fr.devlille.partners.connect.internal.infrastructure.migrations.versions

import fr.devlille.partners.connect.internal.infrastructure.migrations.Migration
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipsTable
import org.jetbrains.exposed.v1.jdbc.SchemaUtils

/**
 * Migration to add communication fields to partnerships table.
 * Adds nullable columns for publication date and support URL.
 */
object AddPartnershipCommunicationFieldsMigration : Migration {
    override val id = "20250804_add_partnership_communication_fields"
    override val description = "Add communication_publication_date and communication_support_url to partnerships"

    override fun up() {
        // Add missing columns to PartnershipsTable - this will add the new columns defined in the table
        SchemaUtils.createMissingTablesAndColumns(PartnershipsTable)
    }

    override fun down() {
        throw UnsupportedOperationException(
            "Rollback not supported for this migration - would require dropping columns which could cause data loss",
        )
    }
}
