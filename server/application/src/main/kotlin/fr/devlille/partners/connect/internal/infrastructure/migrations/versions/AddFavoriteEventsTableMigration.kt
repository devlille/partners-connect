package fr.devlille.partners.connect.internal.infrastructure.migrations.versions

import fr.devlille.partners.connect.events.infrastructure.db.FavoriteEventsTable
import fr.devlille.partners.connect.internal.infrastructure.migrations.Migration
import org.jetbrains.exposed.v1.jdbc.SchemaUtils

object AddFavoriteEventsTableMigration : Migration {
    override val id = "20260524_add_favorite_events"
    override val description = "Add favorite_events join table linking users to events they have starred"

    override fun up() {
        SchemaUtils.createMissingTablesAndColumns(FavoriteEventsTable)
    }

    override fun down() {
        throw UnsupportedOperationException(
            "Rollback not supported for this migration - would require dropping table with potential data loss",
        )
    }
}
