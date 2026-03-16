package fr.devlille.partners.connect.internal.infrastructure.migrations.versions

import fr.devlille.partners.connect.internal.infrastructure.migrations.Migration
import fr.devlille.partners.connect.partnership.infrastructure.db.CommunicationPlansTable
import org.jetbrains.exposed.v1.jdbc.SchemaUtils

object CreateCommunicationPlansTableMigration : Migration {
    override val id = "20260316_create_communication_plans_table"
    override val description = "Create communication_plans table"

    override fun up() {
        SchemaUtils.create(CommunicationPlansTable)
    }

    override fun down() {
        throw UnsupportedOperationException(
            "Rollback not supported — dropping communication_plans would cause data loss",
        )
    }
}
