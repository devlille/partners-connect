package fr.devlille.partners.connect.partnership.factories

import fr.devlille.partners.connect.partnership.infrastructure.db.QandaAnswerEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.QandaQuestionEntity
import java.util.UUID

fun insertMockedQandaAnswer(
    id: UUID = UUID.randomUUID(),
    questionId: UUID,
    answer: String = id.toString(),
    isCorrect: Boolean = false,
): QandaAnswerEntity = QandaAnswerEntity.new(id) {
    this.questionEntity = QandaQuestionEntity[questionId]
    this.answer = answer
    this.isCorrect = isCorrect
}
