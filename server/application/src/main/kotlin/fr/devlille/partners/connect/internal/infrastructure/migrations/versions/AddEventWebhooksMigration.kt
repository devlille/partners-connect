package fr.devlille.partners.connect.internal.infrastructure.migrations.versions

import fr.devlille.partners.connect.integrations.infrastructure.db.WebhookIntegrationsTable
import fr.devlille.partners.connect.internal.infrastructure.migrations.Migration
import org.jetbrains.exposed.v1.jdbc.SchemaUtils

/**
 * Migration to add the webhook_integrations table for storing webhook configurations as integrations.
 */
object AddEventWebhooksMigration : Migration {
    override val id = "20250807_add_event_webhooks"
    override val description = "Add webhook_integrations table for storing webhook configurations as integrations"

    override fun up() {
        SchemaUtils.createMissingTablesAndColumns(WebhookIntegrationsTable)
    }

    override fun down() {
        throw UnsupportedOperationException(
            "Rollback not supported for this migration - would require dropping table with potential data loss",
        )
    }
}
