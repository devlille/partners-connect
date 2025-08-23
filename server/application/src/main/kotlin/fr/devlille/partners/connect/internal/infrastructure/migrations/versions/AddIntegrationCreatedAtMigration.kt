package fr.devlille.partners.connect.internal.infrastructure.migrations.versions

import fr.devlille.partners.connect.integrations.infrastructure.db.IntegrationsTable
import fr.devlille.partners.connect.internal.infrastructure.migrations.Migration
import org.jetbrains.exposed.v1.jdbc.SchemaUtils

/**
 * Migration to add the created_at column to integrations table for audit tracking.
 */
object AddIntegrationCreatedAtMigration : Migration {
    override val id = "20250808_add_integration_created_at"
    override val description = "Add created_at column to integrations table for audit tracking"

    override fun up() {
        SchemaUtils.createMissingTablesAndColumns(IntegrationsTable)
    }

    override fun down() {
        throw UnsupportedOperationException(
            "Rollback not supported for this migration - would require dropping column with potential data loss",
        )
    }
}
