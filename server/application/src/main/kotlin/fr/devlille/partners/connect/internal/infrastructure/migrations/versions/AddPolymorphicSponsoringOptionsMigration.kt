package fr.devlille.partners.connect.internal.infrastructure.migrations.versions

import fr.devlille.partners.connect.internal.infrastructure.migrations.Migration
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipOptionsTable
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SelectableValuesTable
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringOptionsTable
import org.jetbrains.exposed.v1.jdbc.SchemaUtils

/**
 * Migration to add polymorphic sponsoring options support.
 * Adds new columns to existing tables and creates SelectableValuesTable.
 *
 * This migration adds:
 * - optionType, quantitativeDescriptor, numberDescriptor, selectableDescriptor, fixedQuantity columns
 *   to SponsoringOptionsTable
 * - selectedQuantity, selectedValue columns to PartnershipOptionsTable
 * - New SelectableValuesTable for selectable option values
 */
object AddPolymorphicSponsoringOptionsMigration : Migration {
    override val id = "20251101_add_polymorphic_sponsoring_options"
    override val description = "Add polymorphic sponsoring options with four types: " +
        "TEXT, TYPED_QUANTITATIVE, TYPED_NUMBER, TYPED_SELECTABLE"

    override fun up() {
        // Create new table for selectable values
        SchemaUtils.createMissingTablesAndColumns(SelectableValuesTable)

        // Add new columns to existing tables (SchemaUtils.createMissingTablesAndColumns handles this)
        SchemaUtils.createMissingTablesAndColumns(
            SponsoringOptionsTable,
            PartnershipOptionsTable,
        )
    }
}
