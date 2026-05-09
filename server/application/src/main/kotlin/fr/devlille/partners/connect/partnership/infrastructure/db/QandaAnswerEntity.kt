package fr.devlille.partners.connect.partnership.infrastructure.db

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.util.UUID

class QandaAnswerEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<QandaAnswerEntity>(QandaAnswersTable) {
        fun countByPartnerships(partnershipIds: Set<UUID>): Map<UUID, Int> = QandaAnswersTable
            .innerJoin(QandaQuestionsTable)
            .selectAll()
            .where { QandaQuestionsTable.partnershipId inList partnershipIds }
            .groupingBy { it[QandaQuestionsTable.partnershipId].value }
            .eachCount()
    }

    var questionEntity by QandaQuestionEntity referencedOn QandaAnswersTable.questionId
    var answer by QandaAnswersTable.answer
    var isCorrect by QandaAnswersTable.isCorrect
}
