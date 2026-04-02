package fr.devlille.partners.connect.partnership.factories

import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.QandaQuestionEntity
import java.util.UUID

fun insertMockedQandaQuestion(
    id: UUID = UUID.randomUUID(),
    partnershipId: UUID,
    question: String = id.toString(),
): QandaQuestionEntity = QandaQuestionEntity.new(id) {
    this.partnership = PartnershipEntity[partnershipId]
    this.question = question
}
