package fr.devlille.partners.connect.internal.infrastructure.migrations

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.datetime

/**
 * Table to track applied migrations
 */
object MigrationsTable : UUIDTable("schema_migrations") {
    private const val MIGRATION_ID_LENGTH = 255
    val migrationId = varchar("migration_id", MIGRATION_ID_LENGTH).uniqueIndex()
    val description = text("description")
    val appliedAt = datetime("applied_at").clientDefault {
        Clock.System.now().toLocalDateTime(TimeZone.UTC)
    }
}
