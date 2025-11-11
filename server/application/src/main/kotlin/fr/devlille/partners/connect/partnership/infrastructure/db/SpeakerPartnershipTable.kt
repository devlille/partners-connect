package fr.devlille.partners.connect.partnership.infrastructure.db

import fr.devlille.partners.connect.agenda.infrastructure.db.SpeakersTable
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable
import org.jetbrains.exposed.v1.datetime.datetime

/**
 * Database table for speaker-partnership associations.
 *
 * This table creates a many-to-many relationship between speakers and partnerships,
 * allowing speakers to be attached to approved partnerships for organizer visibility.
 *
 * Database constraints:
 * - speakerId: foreign key to SpeakersTable (non-null)
 * - partnershipId: foreign key to PartnershipsTable (non-null)
 * - createdAt: auto-populated timestamp in UTC
 * - Composite unique index on (speakerId, partnershipId) prevents duplicates
 */
object SpeakerPartnershipTable : UUIDTable("speaker_partnerships") {
    /** Foreign key reference to the speaker (required) */
    val speakerId = reference("speaker_id", SpeakersTable)

    /** Foreign key reference to the partnership (required) */
    val partnershipId = reference("partnership_id", PartnershipsTable)

    /** Timestamp when the speaker was attached to the partnership (auto-populated) */
    val createdAt = datetime("created_at").clientDefault {
        Clock.System.now().toLocalDateTime(TimeZone.UTC)
    }

    init {
        // Create composite unique index to prevent duplicate speaker-partnership attachments
        uniqueIndex(speakerId, partnershipId)
    }
}
