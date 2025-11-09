@file:Suppress("MagicNumber")

package fr.devlille.partners.connect.provider.infrastructure.db

import fr.devlille.partners.connect.organisations.infrastructure.db.OrganisationsTable
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.datetime

/**
 * Database table definition for provider entities.
 *
 * Providers are organisation-scoped entities representing service or product providers
 * that can be attached to events for partnership management. Each provider belongs to
 * exactly one organisation and maintains contact information and categorization.
 *
 * Database constraints:
 * - name: non-null, max 255 characters
 * - type: non-null, max 100 characters
 * - website: nullable text field for URLs
 * - phone: nullable, max 30 characters
 * - email: nullable, max 255 characters
 * - organisationId: foreign key to OrganisationsTable (non-null)
 * - createdAt: auto-populated timestamp in UTC
 */
object ProvidersTable : UUIDTable("providers") {
    /** Display name of the provider (required) */
    val name = varchar("name", 255)

    /** Category or type of provider (required) */
    val type = varchar("type", 100)

    /** Optional website URL for the provider */
    val website = text("website").nullable()

    /** Optional contact phone number */
    val phone = varchar("phone", 30).nullable()

    /** Optional contact email address */
    val email = varchar("email", 255).nullable()

    /** Foreign key reference to the owning organisation (required) */
    val organisationId = reference("organisation_id", OrganisationsTable)

    /** Timestamp when the provider was created (auto-populated) */
    val createdAt = datetime("created_at").clientDefault {
        Clock.System.now().toLocalDateTime(TimeZone.UTC)
    }
}
