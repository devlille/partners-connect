package fr.devlille.partners.connect.internal.infrastructure.migrations.versions

import fr.devlille.partners.connect.internal.infrastructure.migrations.Migration
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipOptionsTable
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipsTable
import org.jetbrains.exposed.v1.jdbc.SchemaUtils

/**
 * Migration to add price override capability to partnerships.
 * Adds nullable pack_price_override column to partnerships table and
 * nullable price_override column to partnership_options table.
 */
object AddPartnershipPriceOverridesMigration : Migration {
    override val id = "20260227_add_partnership_price_overrides"
    override val description = "Add pack_price_override to partnerships and price_override to partnership_options"

    override fun up() {
        SchemaUtils.createMissingTablesAndColumns(PartnershipsTable)
        SchemaUtils.createMissingTablesAndColumns(PartnershipOptionsTable)
    }

    override fun down() {
        throw UnsupportedOperationException(
            "Rollback not supported - would require dropping columns which could cause data loss",
        )
    }
}
