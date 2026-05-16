package fr.devlille.partners.connect.internal.infrastructure.migrations.versions

import fr.devlille.partners.connect.ecosystempartners.infrastructure.db.EcosystemPartnersTable
import fr.devlille.partners.connect.internal.infrastructure.migrations.Migration
import org.jetbrains.exposed.v1.jdbc.SchemaUtils

object CreateEcosystemPartnersTableMigration : Migration {
    override val id = "20260516_create_ecosystem_partners_table"
    override val description = "Create ecosystem_partners table (curated non-contractual partners per event)"

    override fun up() {
        SchemaUtils.createMissingTablesAndColumns(EcosystemPartnersTable)
    }
}
