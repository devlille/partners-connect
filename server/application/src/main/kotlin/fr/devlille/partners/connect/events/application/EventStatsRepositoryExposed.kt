package fr.devlille.partners.connect.events.application

import fr.devlille.partners.connect.companies.infrastructure.db.CompanyJobOfferPromotionEntity
import fr.devlille.partners.connect.companies.infrastructure.db.CompanySocialEntity
import fr.devlille.partners.connect.events.domain.EventStats
import fr.devlille.partners.connect.events.domain.EventStatsRepository
import fr.devlille.partners.connect.events.domain.JobOfferStats
import fr.devlille.partners.connect.events.domain.PartnerStats
import fr.devlille.partners.connect.events.domain.QandaStats
import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.events.infrastructure.db.findBySlug
import fr.devlille.partners.connect.internal.infrastructure.db.PromotionStatus
import fr.devlille.partners.connect.partnership.application.mappers.toDomain
import fr.devlille.partners.connect.partnership.infrastructure.db.BoothActivityEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.CommunicationPlanEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEmailEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipTicketEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipsTable
import fr.devlille.partners.connect.partnership.infrastructure.db.QandaAnswerEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.QandaQuestionEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.SpeakerPartnershipEntity
import io.ktor.server.plugins.NotFoundException
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class EventStatsRepositoryExposed : EventStatsRepository {
    override fun findByEventSlug(eventSlug: String): EventStats = transaction {
        val event = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event with slug $eventSlug not found")

        val partnerships = PartnershipEntity
            .filters(
                eventId = event.id.value,
                packId = null,
                validated = null,
                suggestion = null,
                agreementGenerated = null,
                agreementSigned = null,
                organiserUserId = null,
                declined = false,
            )
            .orderBy(PartnershipsTable.createdAt to SortOrder.ASC)
            .toList()

        if (partnerships.isEmpty()) {
            return@transaction EventStats(partners = emptyList())
        }

        val partnershipIds = partnerships.map { it.id.value }.toSet()
        val companyIds = partnerships.map { it.company.id.value }.toSet()

        val activities = BoothActivityEntity.countByPartnerships(partnershipIds)
        val tickets = PartnershipTicketEntity.countByPartnerships(partnershipIds)
        val communicationPlans = CommunicationPlanEntity.countByPartnerships(partnershipIds)
        val speakers = SpeakerPartnershipEntity.countByPartnerships(partnershipIds)
        val questions = QandaQuestionEntity.countByPartnerships(partnershipIds)
        val answers = QandaAnswerEntity.countByPartnerships(partnershipIds)
        val totalJobOffers = CompanyJobOfferPromotionEntity.countByPartnerships(partnershipIds)
        val approvedJobOffers = CompanyJobOfferPromotionEntity
            .countByPartnerships(partnershipIds, PromotionStatus.APPROVED)
        val socialLinks = CompanySocialEntity.countByCompanies(companyIds)

        val partners = partnerships.map { partnership ->
            val pid = partnership.id.value
            val cid = partnership.company.id.value
            PartnerStats(
                partnership = partnership.toDomain(PartnershipEmailEntity.emails(pid)),
                jobOffers = JobOfferStats(
                    total = totalJobOffers[pid] ?: 0,
                    validated = approvedJobOffers[pid] ?: 0,
                ),
                activities = activities[pid] ?: 0,
                qanda = QandaStats(
                    questions = questions[pid] ?: 0,
                    answers = answers[pid] ?: 0,
                ),
                tickets = tickets[pid] ?: 0,
                socialLinks = socialLinks[cid] ?: 0,
                communicationPlan = communicationPlans[pid] ?: 0,
                speakers = speakers[pid] ?: 0,
            )
        }

        EventStats(partners = partners)
    }
}
