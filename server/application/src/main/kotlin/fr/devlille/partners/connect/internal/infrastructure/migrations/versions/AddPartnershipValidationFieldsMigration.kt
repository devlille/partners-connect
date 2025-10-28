package fr.devlille.partners.connect.internal.infrastructure.migrations.versions

import fr.devlille.partners.connect.internal.infrastructure.migrations.Migration
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipsTable
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringPacksTable
import org.jetbrains.exposed.v1.jdbc.SchemaUtils

/**
 * Migration to support partnership validation with customizable package details.
 *
 * Changes to sponsoring_packs table:
 * - Adds booth_size (TEXT, nullable) column to store booth dimensions as string (e.g., "3x3m", "6x3m")
 * - Drops with_booth (BOOLEAN) column as it's replaced by the more flexible booth_size field
 *
 * Changes to partnerships table:
 * - Adds validated_nb_tickets (INTEGER, nullable) to store approved ticket count
 * - Adds validated_nb_job_offers (INTEGER, nullable) to store approved job offer count
 * - Adds validated_booth_size (TEXT, nullable) to store approved booth dimensions
 *
 * These fields enable organizers to validate partnership details that may differ from
 * the base sponsoring pack specifications.
 *
 * IMPORTANT: The with_booth column must be manually dropped after this migration runs:
 * ```sql
 * ALTER TABLE sponsoring_packs DROP COLUMN with_booth;
 * ```
 * This manual step ensures safer production deployment as the column can be removed
 * after verifying the new booth_size column is functioning correctly.
 */
object AddPartnershipValidationFieldsMigration : Migration {
    override val id = "20251028_000000_add_partnership_validation_fields"
    override val description =
        "Add validation fields to partnerships and booth_size to sponsoring_packs (manual: drop with_booth)"

    override fun up() {
        // Add new columns to partnerships table
        SchemaUtils.createMissingTablesAndColumns(PartnershipsTable)

        // Add booth_size column to sponsoring_packs table
        SchemaUtils.createMissingTablesAndColumns(SponsoringPacksTable)

        // NOTE: with_booth column should be dropped manually after deployment:
        // ALTER TABLE sponsoring_packs DROP COLUMN with_booth;
    }

    override fun down() {
        throw UnsupportedOperationException(
            "Rollback not supported for this migration - " +
                "would require recreating with_booth column and dropping booth_size, " +
                "with potential data loss for existing booth size specifications",
        )
    }
}
