package fr.devlille.partners.connect.internal.infrastructure.migrations.versions

import fr.devlille.partners.connect.internal.infrastructure.migrations.Migration
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringPacksTable
import org.jetbrains.exposed.v1.jdbc.SchemaUtils

object AddFlyerTemplateColumnsToSponsoringPacksMigration : Migration {
    override val id = "20260516_add_flyer_template_columns_to_sponsoring_packs"
    override val description =
        "Add nullable flyer_template_url and flyer_zone_x/y/width/height columns to sponsoring_packs"

    override fun up() {
        SchemaUtils.createMissingTablesAndColumns(SponsoringPacksTable)
    }
}
