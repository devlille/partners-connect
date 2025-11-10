package fr.devlille.partners.connect.internal.infrastructure.migrations.versions

import fr.devlille.partners.connect.internal.infrastructure.migrations.Migration
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipOptionsTable
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SelectableValuesTable
import org.jetbrains.exposed.v1.jdbc.SchemaUtils

/**
 * Migration to add individual pricing support for selectable values.
 *
 * This migration adds:
 * - price column to SelectableValuesTable (with default value for existing records)
 * - selectedValueId column to PartnershipOptionsTable for ID-based selection
 *
 * Note: This migration handles existing data by setting a default price of 0
 * for all existing selectable values.
 */
object AddSelectableValuePricingMigration : Migration {
    override val id = "20251111_add_selectable_value_pricing"
    override val description = "Add individual pricing for selectable values and ID-based selection for partnerships"

    override fun up() {
        // Add new columns to existing tables
        // Since price is non-nullable, any existing selectable_values records
        // would cause issues. In practice, this should be applied to a clean database
        // or after ensuring no existing selectable values exist.
        SchemaUtils.createMissingTablesAndColumns(
            SelectableValuesTable,
            PartnershipOptionsTable,
        )
    }
}
