package fr.devlille.partners.connect.internal.infrastructure.migrations.versions

import fr.devlille.partners.connect.ecosystempartners.infrastructure.db.EcosystemPartnerCategoriesTable
import fr.devlille.partners.connect.internal.infrastructure.migrations.Migration
import org.jetbrains.exposed.v1.jdbc.SchemaUtils

object CreateEcosystemPartnerCategoriesTableMigration : Migration {
    override val id = "20260516_create_ecosystem_partner_categories_table"
    override val description = "Create per-event ecosystem partner categories table"

    override fun up() {
        SchemaUtils.createMissingTablesAndColumns(EcosystemPartnerCategoriesTable)
    }
}
