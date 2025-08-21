package fr.devlille.partners.connect.internal.infrastructure.migrations.versions

import fr.devlille.partners.connect.events.infrastructure.db.EventExternalLinksTable
import fr.devlille.partners.connect.internal.infrastructure.migrations.Migration
import org.jetbrains.exposed.v1.jdbc.SchemaUtils

/**
 * Migration to add the event_external_links table for storing external links attached to events.
 */
object AddEventExternalLinksMigration : Migration {
    override val id = "20250107_120000_add_event_external_links"
    override val description = "Add event_external_links table for storing external links attached to events"

    override fun up() {
        SchemaUtils.createMissingTablesAndColumns(EventExternalLinksTable)
    }

    override fun down() {
        throw UnsupportedOperationException(
            "Rollback not supported for this migration - would require dropping table with potential data loss",
        )
    }
}
