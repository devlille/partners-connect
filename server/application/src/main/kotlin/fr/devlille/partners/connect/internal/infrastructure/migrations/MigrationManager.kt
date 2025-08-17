package fr.devlille.partners.connect.internal.infrastructure.migrations

import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.slf4j.LoggerFactory

/**
 * Manages database migrations, ensuring they are applied in the correct order
 * and tracking which migrations have been applied.
 */
class MigrationManager(
    private val migrations: List<Migration>,
) {
    private val logger = LoggerFactory.getLogger(MigrationManager::class.java)

    /**
     * Apply all pending migrations to the database
     */
    fun migrate(database: Database) {
        transaction(database) {
            // Ensure migrations table exists
            SchemaUtils.createMissingTablesAndColumns(MigrationsTable)

            // Get applied migrations
            val appliedMigrations = MigrationsTable
                .selectAll()
                .map { it[MigrationsTable.migrationId] }
                .toSet()

            // Sort migrations by ID to ensure they're applied in order
            val sortedMigrations = migrations.sortedBy { it.id }

            // Apply pending migrations
            sortedMigrations.forEach { migration ->
                if (migration.id !in appliedMigrations) {
                    logger.info("Applying migration: ${migration.id} - ${migration.description}")

                    @Suppress("TooGenericExceptionCaught")
                    try {
                        migration.up()

                        // Record that this migration was applied
                        MigrationsTable.insert {
                            it[migrationId] = migration.id
                            it[description] = migration.description
                        }

                        logger.info("Successfully applied migration: ${migration.id}")
                    } catch (sqlException: java.sql.SQLException) {
                        logger.error("Database error applying migration: ${migration.id}", sqlException)
                        throw IllegalStateException(
                            "Migration ${migration.id} failed with SQL error: ${sqlException.message}",
                            sqlException,
                        )
                    } catch (migrationException: RuntimeException) {
                        logger.error("Runtime error applying migration: ${migration.id}", migrationException)
                        throw IllegalStateException(
                            "Migration ${migration.id} failed: ${migrationException.message}",
                            migrationException,
                        )
                    }
                } else {
                    logger.debug("Skipping already applied migration: ${migration.id}")
                }
            }

            logger.info("All migrations applied successfully")
        }
    }

    /**
     * Get list of applied migrations
     */
    fun getAppliedMigrations(database: Database): List<String> {
        return transaction(database) {
            // Ensure migrations table exists first
            SchemaUtils.createMissingTablesAndColumns(MigrationsTable)

            MigrationsTable
                .selectAll()
                .orderBy(MigrationsTable.appliedAt)
                .map { it[MigrationsTable.migrationId] }
        }
    }

    /**
     * Check if all migrations have been applied
     */
    fun isPendingMigrations(database: Database): Boolean {
        val appliedMigrations = getAppliedMigrations(database).toSet()
        return migrations.any { it.id !in appliedMigrations }
    }
}
