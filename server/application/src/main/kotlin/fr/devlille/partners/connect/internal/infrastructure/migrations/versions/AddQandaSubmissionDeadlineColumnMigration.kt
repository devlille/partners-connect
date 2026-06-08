package fr.devlille.partners.connect.internal.infrastructure.migrations.versions

import fr.devlille.partners.connect.events.infrastructure.db.EventsTable
import fr.devlille.partners.connect.internal.infrastructure.migrations.Migration
import org.jetbrains.exposed.v1.jdbc.SchemaUtils

object AddQandaSubmissionDeadlineColumnMigration : Migration {
    override val id = "20260608_add_qanda_submission_deadline_column"
    override val description = "Add qanda_submission_deadline column to events table"

    override fun up() {
        SchemaUtils.createMissingTablesAndColumns(EventsTable)
    }
}
