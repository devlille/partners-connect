package fr.devlille.partners.connect.partnership.domain

import java.util.UUID

interface QandaRepository {
    fun listByPartnership(partnershipId: UUID): List<QandaQuestion>

    fun listByEvent(eventSlug: String): List<PartnershipQandaSummary>

    fun create(partnershipId: UUID, eventSlug: String, request: QandaQuestionRequest): QandaQuestion

    fun update(partnershipId: UUID, questionId: UUID, eventSlug: String, request: QandaQuestionRequest): QandaQuestion

    fun delete(partnershipId: UUID, questionId: UUID)
}
