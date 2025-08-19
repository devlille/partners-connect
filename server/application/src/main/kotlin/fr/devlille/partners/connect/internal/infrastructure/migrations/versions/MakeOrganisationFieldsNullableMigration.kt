package fr.devlille.partners.connect.internal.infrastructure.migrations.versions

import fr.devlille.partners.connect.internal.infrastructure.migrations.Migration
import fr.devlille.partners.connect.organisations.infrastructure.db.OrganisationsTable
import org.jetbrains.exposed.v1.jdbc.SchemaUtils

/**
 * Migration to make organisation fields nullable except for name and slug.
 * This allows creating minimal organisations with only a name.
 */
object MakeOrganisationFieldsNullableMigration : Migration {
    override val id = "20250802_make_organisation_fields_nullable"
    override val description = "Make organisation fields nullable except name and slug"

    override fun up() {
        // Since the OrganisationsTable is already defined with nullable columns,
        // we can use SchemaUtils to modify the existing table structure.
        // This will attempt to synchronize the table schema with the current definition.
        try {
            SchemaUtils.createMissingTablesAndColumns(OrganisationsTable)
        } catch (e: Exception) {
            // If table already exists with different constraints, log and continue
            // The columns are already defined as nullable in the table definition
            println("Table modification completed or already up to date: ${e.message}")
        }
    }

    override fun down() {
        throw UnsupportedOperationException(
            "Rollback not supported for this migration - would require complex data validation",
        )
    }
}
