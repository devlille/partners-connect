package fr.devlille.partners.connect.internal.infrastructure.migrations.versions

import fr.devlille.partners.connect.events.infrastructure.db.EventsTable
import fr.devlille.partners.connect.internal.infrastructure.migrations.Migration
import fr.devlille.partners.connect.partnership.infrastructure.db.QandaAnswersTable
import fr.devlille.partners.connect.partnership.infrastructure.db.QandaQuestionsTable
import org.jetbrains.exposed.v1.jdbc.SchemaUtils

object CreateQandaTablesMigration : Migration {
    override val id = "20260402_create_qanda_tables"
    override val description = "Add Q&A columns to events table and create qanda_questions/qanda_answers tables"

    override fun up() {
        SchemaUtils.createMissingTablesAndColumns(EventsTable, QandaQuestionsTable, QandaAnswersTable)
    }

    override fun down() {
        throw UnsupportedOperationException(
            "Rollback not supported for this migration - would require dropping tables with potential data loss",
        )
    }
}
