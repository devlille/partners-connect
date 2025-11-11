package fr.devlille.partners.connect.internal.infrastructure.migrations.versions

import fr.devlille.partners.connect.agenda.infrastructure.db.SessionsTable
import fr.devlille.partners.connect.agenda.infrastructure.db.SpeakersTable
import fr.devlille.partners.connect.integrations.infrastructure.db.OpenPlannerIntegrationsTable
import fr.devlille.partners.connect.internal.infrastructure.migrations.Migration
import fr.devlille.partners.connect.partnership.infrastructure.db.SpeakerPartnershipTable
import org.jetbrains.exposed.v1.jdbc.SchemaUtils

object AddAgendaTablesMigration : Migration {
    override val id = "20251112_add_agenda_tables"
    override val description = "Add speakers, sessions, and speaker-partnership tables"

    override fun up() {
        SchemaUtils.createMissingTablesAndColumns(
            OpenPlannerIntegrationsTable,
            SpeakersTable,
            SessionsTable,
            SpeakerPartnershipTable,
        )
    }
}
