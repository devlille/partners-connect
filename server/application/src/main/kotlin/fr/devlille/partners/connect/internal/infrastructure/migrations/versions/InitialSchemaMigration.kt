package fr.devlille.partners.connect.internal.infrastructure.migrations.versions

import fr.devlille.partners.connect.companies.infrastructure.db.CompaniesTable
import fr.devlille.partners.connect.companies.infrastructure.db.CompanySocialsTable
import fr.devlille.partners.connect.events.infrastructure.db.EventsTable
import fr.devlille.partners.connect.integrations.infrastructure.db.BilletWebIntegrationsTable
import fr.devlille.partners.connect.integrations.infrastructure.db.IntegrationsTable
import fr.devlille.partners.connect.integrations.infrastructure.db.MailjetIntegrationsTable
import fr.devlille.partners.connect.integrations.infrastructure.db.QontoIntegrationsTable
import fr.devlille.partners.connect.integrations.infrastructure.db.SlackIntegrationsTable
import fr.devlille.partners.connect.internal.infrastructure.migrations.Migration
import fr.devlille.partners.connect.organisations.infrastructure.db.OrganisationsTable
import fr.devlille.partners.connect.partnership.infrastructure.db.BillingsTable
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEmailsTable
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipOptionsTable
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipTicketsTable
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipsTable
import fr.devlille.partners.connect.sponsoring.infrastructure.db.OptionTranslationsTable
import fr.devlille.partners.connect.sponsoring.infrastructure.db.PackOptionsTable
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringOptionsTable
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringPacksTable
import fr.devlille.partners.connect.users.infrastructure.db.OrganisationPermissionsTable
import fr.devlille.partners.connect.users.infrastructure.db.UsersTable
import org.jetbrains.exposed.v1.jdbc.SchemaUtils

/**
 * Initial migration that creates all the existing tables.
 * This represents the current state of the database schema.
 */
object InitialSchemaMigration : Migration {
    override val id = "20250801_initial_schema"
    override val description = "Create initial database schema with all tables"

    override fun up() {
        SchemaUtils.create(
            // integrations
            IntegrationsTable,
            SlackIntegrationsTable,
            MailjetIntegrationsTable,
            QontoIntegrationsTable,
            BilletWebIntegrationsTable,
            // organisations
            OrganisationsTable,
            // events
            EventsTable,
            // users
            UsersTable,
            OrganisationPermissionsTable,
            // sponsoring
            PackOptionsTable,
            SponsoringOptionsTable,
            SponsoringPacksTable,
            OptionTranslationsTable,
            // companies
            CompaniesTable,
            CompanySocialsTable,
            BillingsTable,
            // partnerships
            PartnershipsTable,
            PartnershipOptionsTable,
            PartnershipEmailsTable,
            PartnershipTicketsTable,
        )
    }
}
