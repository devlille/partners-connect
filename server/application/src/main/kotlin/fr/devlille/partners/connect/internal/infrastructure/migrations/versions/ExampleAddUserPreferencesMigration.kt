@file:Suppress("MagicNumber")

package fr.devlille.partners.connect.internal.infrastructure.migrations.versions

import fr.devlille.partners.connect.internal.infrastructure.migrations.Migration
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.datetime
import org.jetbrains.exposed.v1.jdbc.SchemaUtils

/**
 * Example migration showing how to add a new table.
 * This migration is commented out in the registry and serves as documentation.
 *
 * Migration naming convention: YYYYMMDD_HHMMSS_description
 * - Use UTC timestamp for when you create the migration
 * - Use snake_case for description
 * - Keep description concise but descriptive
 */
object ExampleAddUserPreferencesMigration : Migration {
    override val id = "20241220_130000_add_user_preferences_table"
    override val description = "Add user_preferences table to store user-specific settings"

    override fun up() {
        SchemaUtils.create(UserPreferencesTable)
    }

    override fun down() {
        SchemaUtils.drop(UserPreferencesTable)
    }
}

/**
 * Example table definition for the migration
 */
private object UserPreferencesTable : UUIDTable("user_preferences") {
    val userId = reference("user_id", fr.devlille.partners.connect.users.infrastructure.db.UsersTable)
    val theme = varchar("theme", 50).default("light")
    val language = varchar("language", 10).default("en")
    val notificationsEnabled = bool("notifications_enabled").default(true)
    val createdAt = datetime("created_at").clientDefault {
        Clock.System.now().toLocalDateTime(TimeZone.UTC)
    }
    val updatedAt = datetime("updated_at").clientDefault {
        Clock.System.now().toLocalDateTime(TimeZone.UTC)
    }

    init {
        uniqueIndex(userId)
    }
}
