package fr.devlille.partners.connect.internal.infrastructure.migrations.versions

import fr.devlille.partners.connect.internal.infrastructure.migrations.Migration
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipSupportVideosTable
import org.jetbrains.exposed.v1.jdbc.SchemaUtils

object CreatePartnershipSupportVideosTableMigration : Migration {
    override val id = "20260512_create_partnership_support_videos_table"
    override val description = "Create partnership_support_videos table with PENDING/APPROVED/DECLINED workflow"

    override fun up() {
        SchemaUtils.createMissingTablesAndColumns(PartnershipSupportVideosTable)
    }
}
