package fr.devlille.partners.connect.internal.infrastructure.migrations

/**
 * Represents a database migration that can be applied to evolve the database schema.
 */
interface Migration {
    /**
     * The unique identifier for this migration. Should be formatted as "YYYYMMDD_HHMMSS_description"
     * Example: "20241220_120000_initial_schema"
     */
    val id: String

    /**
     * Human-readable description of what this migration does
     */
    val description: String

    /**
     * Apply this migration. This will be called within a transaction context.
     */
    fun up()

    /**
     * Rollback this migration (optional). This will be called within a transaction context.
     */
    fun down() {
        throw UnsupportedOperationException("Rollback not supported for migration $id")
    }
}
