package fr.devlille.partners.connect.partnership.application.mappers

import fr.devlille.partners.connect.partnership.domain.QandaAnswer
import fr.devlille.partners.connect.partnership.domain.QandaQuestion
import fr.devlille.partners.connect.partnership.infrastructure.db.QandaAnswerEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.QandaQuestionEntity

fun QandaQuestionEntity.toDomain() = QandaQuestion(
    id = id.value.toString(),
    partnershipId = partnership.id.value.toString(),
    question = question,
    answers = answers.map { it.toDomain() },
    createdAt = createdAt,
)

fun QandaAnswerEntity.toDomain() = QandaAnswer(
    id = id.value.toString(),
    answer = answer,
    isCorrect = isCorrect,
)
