package fr.devlille.partners.connect.internal.infrastructure.migrations.versions

import fr.devlille.partners.connect.agenda.infrastructure.db.SpeakersTable
import fr.devlille.partners.connect.internal.infrastructure.migrations.Migration
import org.jetbrains.exposed.v1.jdbc.SchemaUtils

object AddSpeakerSourceColumnMigration : Migration {
    override val id = "20260405_add_speaker_source_column"
    override val description = "Add source column to speakers table to track the integration provider"

    override fun up() {
        SchemaUtils.createMissingTablesAndColumns(SpeakersTable)
    }
}
