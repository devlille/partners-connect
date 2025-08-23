package fr.devlille.partners.connect.internal.infrastructure.migrations.versions

import fr.devlille.partners.connect.events.infrastructure.db.EventWebhooksTable
import fr.devlille.partners.connect.internal.infrastructure.migrations.Migration
import org.jetbrains.exposed.v1.jdbc.SchemaUtils

/**
 * Migration to add the event_webhooks table for storing webhook configurations for events.
 */
object AddEventWebhooksMigration : Migration {
    override val id = "20250106_add_event_webhooks"
    override val description = "Add event_webhooks table for storing webhook configurations for partnership events"

    override fun up() {
        SchemaUtils.createMissingTablesAndColumns(EventWebhooksTable)
    }

    override fun down() {
        throw UnsupportedOperationException(
            "Rollback not supported for this migration - would require dropping table with potential data loss",
        )
    }
}
