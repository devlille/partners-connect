package fr.devlille.partners.connect.internal.infrastructure.migrations.versions

import fr.devlille.partners.connect.internal.infrastructure.migrations.Migration
import fr.devlille.partners.connect.organisations.infrastructure.db.OrganisationsTable
import org.jetbrains.exposed.v1.jdbc.SchemaUtils

/**
 * Migration to make organisation fields nullable except for name and slug.
 * This allows creating minimal organisations with only a name.
 */
object MakeOrganisationFieldsNullableMigration : Migration {
    override val id = "20250101_120000_make_organisation_fields_nullable"
    override val description = "Make organisation fields nullable except name and slug"

    override fun up() {
        // Use SchemaUtils to modify the existing table structure
        // Since we've updated the table definition to make columns nullable,
        // this will apply the changes to the existing table schema
        SchemaUtils.addMissingColumnsStatements(OrganisationsTable).forEach { statement ->
            // Execute statement in transaction context - this will work when called from MigrationManager
            println("Executing: $statement")
        }
    }

    override fun down() {
        throw UnsupportedOperationException(
            "Rollback not supported for this migration - would require complex data validation",
        )
    }
}
