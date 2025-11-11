package fr.devlille.partners.connect.partnership.infrastructure.db

import fr.devlille.partners.connect.agenda.infrastructure.db.SpeakerEntity
import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import java.util.UUID

/**
 * Entity class for speaker-partnership associations.
 *
 * This entity provides type-safe object-relational mapping for the speaker_partnerships table,
 * representing the association between speakers and approved partnerships.
 *
 * Properties:
 * - speaker: Referenced SpeakerEntity from the speakers table
 * - partnership: Referenced PartnershipEntity from the partnerships table
 * - createdAt: Timestamp when the association was created
 */
class SpeakerPartnershipEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<SpeakerPartnershipEntity>(SpeakerPartnershipTable)

    /** The speaker associated with this partnership */
    var speaker by SpeakerEntity referencedOn SpeakerPartnershipTable.speakerId

    /** The partnership associated with this speaker */
    var partnership by PartnershipEntity referencedOn SpeakerPartnershipTable.partnershipId

    /** Timestamp when the speaker-partnership association was created */
    var createdAt by SpeakerPartnershipTable.createdAt
}
