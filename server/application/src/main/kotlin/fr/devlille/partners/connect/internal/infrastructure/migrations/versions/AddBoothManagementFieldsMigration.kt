package fr.devlille.partners.connect.internal.infrastructure.migrations.versions

import fr.devlille.partners.connect.events.infrastructure.db.EventsTable
import fr.devlille.partners.connect.internal.infrastructure.migrations.Migration
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipsTable
import org.jetbrains.exposed.v1.jdbc.SchemaUtils

/**
 * Migration to add booth management fields to events and partnerships tables.
 * Adds booth_plan_image_url to events table and booth_location to partnerships table.
 */
object AddBoothManagementFieldsMigration : Migration {
    override val id = "20250803_add_booth_management_fields"
    override val description =
        "Add booth management fields (booth_plan_image_url to events, booth_location to partnerships)"

    override fun up() {
        // Update the table schemas to include the new nullable columns
        SchemaUtils.createMissingTablesAndColumns(EventsTable, PartnershipsTable)
    }

    override fun down() {
        throw UnsupportedOperationException(
            "Rollback not supported for this migration - would require dropping columns with potential data loss",
        )
    }
}
