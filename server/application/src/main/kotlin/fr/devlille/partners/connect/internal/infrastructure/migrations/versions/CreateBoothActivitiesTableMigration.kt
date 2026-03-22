package fr.devlille.partners.connect.internal.infrastructure.migrations.versions

import fr.devlille.partners.connect.internal.infrastructure.migrations.Migration
import fr.devlille.partners.connect.partnership.infrastructure.db.BoothActivitiesTable
import org.jetbrains.exposed.v1.jdbc.SchemaUtils

object CreateBoothActivitiesTableMigration : Migration {
    override val id = "20260321_create_booth_activities_table"
    override val description = "Create booth_activities table with FK to partnerships and cascade delete"

    override fun up() {
        SchemaUtils.createMissingTablesAndColumns(BoothActivitiesTable)
    }

    override fun down() {
        throw UnsupportedOperationException(
            "Rollback not supported for this migration - would require dropping table with potential data loss",
        )
    }
}
