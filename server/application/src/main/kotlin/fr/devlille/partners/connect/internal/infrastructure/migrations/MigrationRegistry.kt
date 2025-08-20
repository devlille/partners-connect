package fr.devlille.partners.connect.internal.infrastructure.migrations

import fr.devlille.partners.connect.internal.infrastructure.migrations.versions.AddPartnershipCommunicationFieldsMigration
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
        AddPartnershipCommunicationFieldsMigration,
    )

    /**
     * Create a MigrationManager with all registered migrations
     */
    fun createManager(): MigrationManager = MigrationManager(allMigrations)
}
