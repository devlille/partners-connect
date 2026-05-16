package fr.devlille.partners.connect.internal.infrastructure.migrations.versions

import fr.devlille.partners.connect.ecosystempartners.infrastructure.db.EcosystemPartnerEmailsTable
import fr.devlille.partners.connect.internal.infrastructure.migrations.Migration
import org.jetbrains.exposed.v1.jdbc.SchemaUtils

object CreateEcosystemPartnerEmailsTableMigration : Migration {
    override val id = "20260516_create_ecosystem_partner_emails_table"
    override val description = "Create ecosystem_partner_emails table for notification contacts"

    override fun up() {
        SchemaUtils.createMissingTablesAndColumns(EcosystemPartnerEmailsTable)
    }
}
