package fr.devlille.partners.connect.internal.infrastructure.migrations

import fr.devlille.partners.connect.internal.infrastructure.migrations.versions.AddAgendaTablesMigration
import fr.devlille.partners.connect.internal.infrastructure.migrations.versions.AddBoothManagementFieldsMigration
import fr.devlille.partners.connect.internal.infrastructure.migrations.versions.AddCompanyStatusColumnMigration
import fr.devlille.partners.connect.internal.infrastructure.migrations.versions.AddEventExternalLinksMigration
import fr.devlille.partners.connect.internal.infrastructure.migrations.versions.AddEventWebhooksMigration
import fr.devlille.partners.connect.internal.infrastructure.migrations.versions.AddHealthUrlColumnMigration
import fr.devlille.partners.connect.internal.infrastructure.migrations.versions.AddIntegrationCreatedAtMigration
import fr.devlille.partners.connect.internal.infrastructure.migrations.versions.AddPartnershipCommunicationFieldsMigration
import fr.devlille.partners.connect.internal.infrastructure.migrations.versions.AddPartnershipOrganiserMigration
import fr.devlille.partners.connect.internal.infrastructure.migrations.versions.AddPolymorphicSponsoringOptionsMigration
import fr.devlille.partners.connect.internal.infrastructure.migrations.versions.AddProviderManagementMigration
import fr.devlille.partners.connect.internal.infrastructure.migrations.versions.AddSelectableValuePricingMigration
import fr.devlille.partners.connect.internal.infrastructure.migrations.versions.CreateCompanyJobOffersTableMigration
import fr.devlille.partners.connect.internal.infrastructure.migrations.versions.CreateJobOfferPromotionsTableMigration
import fr.devlille.partners.connect.internal.infrastructure.migrations.versions.InitialSchemaMigration
import fr.devlille.partners.connect.internal.infrastructure.migrations.versions.MakeOrganisationFieldsNullableMigration

/**
 * Registry that contains all database migrations in the system.
 * Add new migrations to this list in chronological order.
 */
object MigrationRegistry {
    /**
     * All migrations that should be applied to the database.
     * Migrations are applied in the order they appear in this list.
     */
    val allMigrations: List<Migration> = listOf(
        InitialSchemaMigration,
        MakeOrganisationFieldsNullableMigration,
        AddBoothManagementFieldsMigration,
        AddPartnershipCommunicationFieldsMigration,
        AddEventExternalLinksMigration,
        AddIntegrationCreatedAtMigration,
        AddEventWebhooksMigration,
        CreateCompanyJobOffersTableMigration,
        CreateJobOfferPromotionsTableMigration,
        AddCompanyStatusColumnMigration(),
        AddPolymorphicSponsoringOptionsMigration,
        AddProviderManagementMigration,
        AddSelectableValuePricingMigration,
        AddAgendaTablesMigration,
        AddHealthUrlColumnMigration,
        AddPartnershipOrganiserMigration,
    )

    /**
     * Create a MigrationManager with all registered migrations
     */
    fun createManager(): MigrationManager = MigrationManager(allMigrations)
}
