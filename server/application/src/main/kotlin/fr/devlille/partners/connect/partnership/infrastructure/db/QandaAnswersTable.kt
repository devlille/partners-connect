package fr.devlille.partners.connect.partnership.infrastructure.db

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.UUIDTable

object QandaAnswersTable : UUIDTable("qanda_answers") {
    val questionId = reference("question_id", QandaQuestionsTable, onDelete = ReferenceOption.CASCADE)
    val answer = text("answer")
    val isCorrect = bool("is_correct").default(false)
}
