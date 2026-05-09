package fr.devlille.partners.connect.partnership.infrastructure.db

import org.jetbrains.exposed.v1.core.dao.id.EntityID
import org.jetbrains.exposed.v1.dao.UUIDEntity
import org.jetbrains.exposed.v1.dao.UUIDEntityClass
import org.jetbrains.exposed.v1.jdbc.selectAll
import java.util.UUID

class QandaQuestionEntity(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<QandaQuestionEntity>(QandaQuestionsTable) {
        fun countByPartnerships(partnershipIds: Set<UUID>): Map<UUID, Int> = QandaQuestionsTable
            .selectAll()
            .where { QandaQuestionsTable.partnershipId inList partnershipIds }
            .groupingBy { it[QandaQuestionsTable.partnershipId].value }
            .eachCount()
    }

    var partnership by PartnershipEntity referencedOn QandaQuestionsTable.partnershipId
    var question by QandaQuestionsTable.question
    var createdAt by QandaQuestionsTable.createdAt
    val answers by QandaAnswerEntity referrersOn QandaAnswersTable.questionId
}
