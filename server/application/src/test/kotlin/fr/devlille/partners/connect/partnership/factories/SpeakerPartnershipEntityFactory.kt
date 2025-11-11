package fr.devlille.partners.connect.partnership.factories

import fr.devlille.partners.connect.agenda.infrastructure.db.SpeakerEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.SpeakerPartnershipEntity
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

fun insertMockedSpeakerPartnership(
    id: UUID = UUID.randomUUID(),
    partnershipId: UUID = UUID.randomUUID(),
    speakerId: UUID = UUID.randomUUID(),
): SpeakerPartnershipEntity = transaction {
    SpeakerPartnershipEntity.new(id) {
        this.partnership = PartnershipEntity[partnershipId]
        this.speaker = SpeakerEntity[speakerId]
    }
}
