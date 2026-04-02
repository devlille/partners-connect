package fr.devlille.partners.connect.partnership.application

import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.internal.infrastructure.api.ConflictException
import fr.devlille.partners.connect.internal.infrastructure.api.ForbiddenException
import fr.devlille.partners.connect.partnership.application.mappers.toDomain
import fr.devlille.partners.connect.partnership.domain.PartnershipQandaSummary
import fr.devlille.partners.connect.partnership.domain.QandaQuestion
import fr.devlille.partners.connect.partnership.domain.QandaQuestionRequest
import fr.devlille.partners.connect.partnership.domain.QandaRepository
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipsTable
import fr.devlille.partners.connect.partnership.infrastructure.db.QandaAnswerEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.QandaAnswersTable
import fr.devlille.partners.connect.partnership.infrastructure.db.QandaQuestionEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.QandaQuestionsTable
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import fr.devlille.partners.connect.events.infrastructure.db.findBySlug as eventFindBySlug

class QandaRepositoryExposed : QandaRepository {
    override fun listByPartnership(partnershipId: UUID): List<QandaQuestion> = transaction {
        PartnershipEntity.findById(partnershipId)
            ?: throw NotFoundException("Partnership $partnershipId not found")

        QandaQuestionEntity
            .find { QandaQuestionsTable.partnershipId eq partnershipId }
            .orderBy(QandaQuestionsTable.createdAt to SortOrder.ASC)
            .map { it.toDomain() }
    }

    override fun listByEvent(eventSlug: String): List<PartnershipQandaSummary> = transaction {
        val eventEntity = EventEntity.eventFindBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")

        if (!eventEntity.qandaEnabled) {
            throw ForbiddenException("Q&A is not enabled for this event")
        }

        val partnerships = PartnershipEntity
            .find { PartnershipsTable.eventId eq eventEntity.id }

        partnerships.mapNotNull { partnership ->
            val questions = QandaQuestionEntity
                .find { QandaQuestionsTable.partnershipId eq partnership.id }
                .orderBy(QandaQuestionsTable.createdAt to SortOrder.ASC)
                .map { it.toDomain() }
            if (questions.isEmpty()) return@mapNotNull null
            PartnershipQandaSummary(
                partnershipId = partnership.id.value.toString(),
                companyName = partnership.company.name,
                questions = questions,
            )
        }
    }

    @Suppress("LongMethod")
    override fun create(
        partnershipId: UUID,
        eventSlug: String,
        request: QandaQuestionRequest,
    ): QandaQuestion = transaction {
        val partnership = PartnershipEntity.findById(partnershipId)
            ?: throw NotFoundException("Partnership $partnershipId not found")

        val eventEntity = EventEntity.eventFindBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")

        if (!eventEntity.qandaEnabled) {
            throw ForbiddenException("Q&A is not enabled for this event")
        }

        validateAnswers(request, eventEntity.qandaMaxAnswers)

        val currentCount = QandaQuestionEntity
            .find { QandaQuestionsTable.partnershipId eq partnershipId }
            .count()
        val maxQuestions = eventEntity.qandaMaxQuestions
        if (maxQuestions != null && currentCount >= maxQuestions) {
            throw ConflictException("Maximum number of questions ($maxQuestions) reached for this partnership")
        }

        val questionEntity = QandaQuestionEntity.new {
            this.partnership = partnership
            this.question = request.question
        }
        request.answers.forEach { answerInput ->
            QandaAnswerEntity.new {
                this.questionEntity = questionEntity
                this.answer = answerInput.answer
                this.isCorrect = answerInput.isCorrect
            }
        }
        questionEntity.toDomain()
    }

    override fun update(
        partnershipId: UUID,
        questionId: UUID,
        eventSlug: String,
        request: QandaQuestionRequest,
    ): QandaQuestion = transaction {
        PartnershipEntity.findById(partnershipId)
            ?: throw NotFoundException("Partnership $partnershipId not found")

        val eventEntity = EventEntity.eventFindBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")

        validateAnswers(request, eventEntity.qandaMaxAnswers)

        val questionEntity = QandaQuestionEntity.findById(questionId)
            ?.takeIf { it.partnership.id.value == partnershipId }
            ?: throw NotFoundException("Question not found")

        questionEntity.question = request.question

        QandaAnswersTable.deleteWhere { QandaAnswersTable.questionId eq questionId }
        request.answers.forEach { answerInput ->
            QandaAnswerEntity.new {
                this.questionEntity = questionEntity
                this.answer = answerInput.answer
                this.isCorrect = answerInput.isCorrect
            }
        }
        questionEntity.toDomain()
    }

    override fun delete(partnershipId: UUID, questionId: UUID): Unit = transaction {
        PartnershipEntity.findById(partnershipId)
            ?: throw NotFoundException("Partnership $partnershipId not found")

        val questionEntity = QandaQuestionEntity.findById(questionId)
            ?.takeIf { it.partnership.id.value == partnershipId }
            ?: throw NotFoundException("Question not found")

        questionEntity.delete()
    }

    private fun validateAnswers(request: QandaQuestionRequest, maxAnswers: Int?) {
        val error = when {
            request.answers.size < 2 -> "At least 2 answers are required"
            maxAnswers != null && request.answers.size > maxAnswers ->
                "Number of answers exceeds the maximum of $maxAnswers"
            request.answers.count { it.isCorrect } != 1 ->
                "Exactly one answer must be marked as correct"
            else -> null
        }
        if (error != null) throw BadRequestException(error)
    }
}
