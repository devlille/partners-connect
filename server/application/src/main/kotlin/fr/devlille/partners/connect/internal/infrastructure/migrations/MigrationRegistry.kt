package fr.devlille.partners.connect.internal.infrastructure.migrations

import fr.devlille.partners.connect.internal.infrastructure.migrations.versions.InitialSchemaMigration

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
        // Add new migrations here in chronological order
        // Example:
        // AddUserPreferencesMigration,
        // UpdateCompanyTableMigration,
    )

    /**
     * Create a MigrationManager with all registered migrations
     */
    fun createManager(): MigrationManager = MigrationManager(allMigrations)
}
