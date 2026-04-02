package fr.devlille.partners.connect.partnership.infrastructure.db

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import java.util.UUID

class QandaAnswerEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<QandaAnswerEntity>(QandaAnswersTable)

    var questionEntity by QandaQuestionEntity referencedOn QandaAnswersTable.questionId
    var answer by QandaAnswersTable.answer
    var isCorrect by QandaAnswersTable.isCorrect
}
