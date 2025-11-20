package fr.devlille.partners.connect.internal.infrastructure.migrations.versions

import fr.devlille.partners.connect.integrations.infrastructure.db.WebhookIntegrationsTable
import fr.devlille.partners.connect.internal.infrastructure.migrations.Migration
import org.jetbrains.exposed.v1.jdbc.SchemaUtils

object AddHealthUrlColumnMigration : Migration {
    override val id = "20251121_add_health_url_column"
    override val description = "Add health_url in webhook integrations tables"

    override fun up() {
        SchemaUtils.createMissingTablesAndColumns(WebhookIntegrationsTable)
    }
}
