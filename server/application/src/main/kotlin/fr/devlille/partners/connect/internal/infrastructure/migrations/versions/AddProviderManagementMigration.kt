package fr.devlille.partners.connect.internal.infrastructure.migrations.versions

import fr.devlille.partners.connect.internal.infrastructure.migrations.Migration
import fr.devlille.partners.connect.provider.infrastructure.db.EventProvidersTable
import fr.devlille.partners.connect.provider.infrastructure.db.ProvidersTable
import org.jetbrains.exposed.v1.jdbc.SchemaUtils

/**
 * Migration to add provider management tables.
 */
object AddProviderManagementMigration : Migration {
    override val id = "20250101_add_provider_management"
    override val description = "Add provider management tables (providers, event_providers)"

    override fun up() {
        SchemaUtils.createMissingTablesAndColumns(
            ProvidersTable,
            EventProvidersTable,
        )
    }
}
