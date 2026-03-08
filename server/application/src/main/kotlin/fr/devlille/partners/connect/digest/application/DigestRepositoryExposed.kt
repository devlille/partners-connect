package fr.devlille.partners.connect.digest.application

import fr.devlille.partners.connect.companies.infrastructure.db.hasCompleteAddress
import fr.devlille.partners.connect.digest.domain.DigestEntry
import fr.devlille.partners.connect.digest.domain.DigestRepository
import fr.devlille.partners.connect.digest.domain.EventDigest
import fr.devlille.partners.connect.events.domain.Contact
import fr.devlille.partners.connect.events.domain.EventDisplay
import fr.devlille.partners.connect.events.domain.EventWithOrganisation
import fr.devlille.partners.connect.events.infrastructure.db.EventEntity
import fr.devlille.partners.connect.events.infrastructure.db.findBySlug
import fr.devlille.partners.connect.internal.infrastructure.system.SystemVarEnv
import fr.devlille.partners.connect.organisations.application.mappers.toItemDomain
import fr.devlille.partners.connect.partnership.infrastructure.db.BillingEntity
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEntity
import io.ktor.server.plugins.NotFoundException
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID

class DigestRepositoryExposed : DigestRepository {
    companion object {
        private const val END_OF_DAY_HOUR = 23
        private const val END_OF_DAY_MINUTE = 59
        private const val END_OF_DAY_SECOND = 59
    }

    override suspend fun queryDigest(eventSlug: String, today: LocalDate): EventDigest = transaction {
        val eventEntity = EventEntity.findBySlug(eventSlug)
            ?: throw NotFoundException("Event $eventSlug not found")

        val event = buildEventWithOrganisation(eventEntity)
        val eventSlug = eventEntity.slug

        EventDigest(
            event = event,
            agreementItems = queryAgreementReady(eventEntity.id.value, eventSlug),
            quoteItems = queryQuoteReady(eventEntity.id.value, eventSlug),
            socialMediaItems = querySocialMediaDue(eventEntity.id.value, today, eventSlug),
        )
    }

    private fun buildEventWithOrganisation(eventEntity: EventEntity): EventWithOrganisation =
        EventWithOrganisation(
            event = EventDisplay(
                id = eventEntity.id.value.toString(),
                slug = eventEntity.slug,
                name = eventEntity.name,
                startTime = eventEntity.startTime,
                endTime = eventEntity.endTime,
                submissionStartTime = eventEntity.submissionStartTime,
                submissionEndTime = eventEntity.submissionEndTime,
                address = eventEntity.address,
                contact = Contact(
                    phone = eventEntity.contactPhone,
                    email = eventEntity.contactEmail,
                ),
                externalLinks = emptyList(),
                providers = emptyList(),
            ),
            organisation = eventEntity.organisation.toItemDomain(),
        )

    private fun buildLink(eventSlug: String, partnershipId: UUID): String =
        "${SystemVarEnv.frontendBaseUrl}/$eventSlug/$partnershipId"

    private fun queryAgreementReady(eventId: UUID, eventSlug: String): List<DigestEntry> =
        PartnershipEntity.findAgreementReady(eventId)
            .filter { it.company.hasCompleteAddress() }
            .map { DigestEntry(it.company.name, buildLink(eventSlug, it.id.value)) }

    private fun queryQuoteReady(eventId: UUID, eventSlug: String): List<DigestEntry> =
        PartnershipEntity.findQuoteReady(eventId)
            .filter {
                it.company.hasCompleteAddress() &&
                    (it.selectedPack?.basePrice ?: 0) > 0 &&
                    BillingEntity.singleByEventAndPartnership(eventId, it.id.value)?.quotePdfUrl == null
            }
            .map { DigestEntry(it.company.name, buildLink(eventSlug, it.id.value)) }

    private fun querySocialMediaDue(eventId: UUID, today: LocalDate, eventSlug: String): List<DigestEntry> {
        val todayYear = today.year
        val todayMonth = today.monthNumber
        val todayDay = today.dayOfMonth
        val startOfDay = LocalDateTime(todayYear, todayMonth, todayDay, 0, 0, 0)
        val endOfDay = LocalDateTime(
            todayYear,
            todayMonth,
            todayDay,
            END_OF_DAY_HOUR,
            END_OF_DAY_MINUTE,
            END_OF_DAY_SECOND,
        )
        return PartnershipEntity.findSocialMediaDue(eventId, startOfDay, endOfDay)
            .map { DigestEntry(it.company.name, buildLink(eventSlug, it.id.value)) }
    }
}
