package fr.devlille.partners.connect.digest.application

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.events.factories.insertMockedFutureEventWithSlug
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.partnership.factories.insertMockedCommunicationPlan
import fr.devlille.partners.connect.partnership.factories.insertMockedPartnership
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringPack
import fr.devlille.partners.connect.users.factories.insertMockedUser
import io.ktor.client.request.get
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock

class DigestSocialMediaTest {
    @Test
    @Suppress("LongMethod")
    fun `socialMediaItems lists only partnerships with a communication plan scheduled today`() = testApplication {
        val today = LocalDate(2026, 5, 25)
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val eventSlug = "event-${UUID.randomUUID()}"
        val packId = UUID.randomUUID()
        val todayCompanyId = UUID.randomUUID()
        val tomorrowCompanyId = UUID.randomUUID()
        val declinedCompanyId = UUID.randomUUID()
        val todayPartnershipId = UUID.randomUUID()
        val tomorrowPartnershipId = UUID.randomUUID()
        val declinedPartnershipId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedFutureEventWithSlug(eventId, slug = eventSlug, orgId = orgId)
                insertMockedSponsoringPack(id = packId, eventId = eventId)
                insertMockedCompany(todayCompanyId)
                insertMockedCompany(tomorrowCompanyId)
                insertMockedCompany(declinedCompanyId)
                insertMockedPartnership(
                    id = todayPartnershipId,
                    eventId = eventId,
                    companyId = todayCompanyId,
                    selectedPackId = packId,
                )
                insertMockedPartnership(
                    id = tomorrowPartnershipId,
                    eventId = eventId,
                    companyId = tomorrowCompanyId,
                    selectedPackId = packId,
                )
                insertMockedPartnership(
                    id = declinedPartnershipId,
                    eventId = eventId,
                    companyId = declinedCompanyId,
                    selectedPackId = packId,
                    declinedAt = Clock.System.now().toLocalDateTime(TimeZone.UTC),
                )
                insertMockedCommunicationPlan(
                    eventId = eventId,
                    partnershipId = todayPartnershipId,
                    scheduledDate = LocalDateTime(2026, 5, 25, 10, 0),
                )
                insertMockedCommunicationPlan(
                    eventId = eventId,
                    partnershipId = tomorrowPartnershipId,
                    scheduledDate = LocalDateTime(2026, 5, 26, 10, 0),
                )
                insertMockedCommunicationPlan(
                    eventId = eventId,
                    partnershipId = declinedPartnershipId,
                    scheduledDate = LocalDateTime(2026, 5, 25, 11, 0),
                )
                insertMockedCommunicationPlan(
                    eventId = eventId,
                    partnershipId = null,
                    scheduledDate = LocalDateTime(2026, 5, 25, 12, 0),
                )
            }
        }
        client.get("/")

        val digest = runBlocking {
            DigestRepositoryExposed().queryDigest(eventSlug, today)
        }

        assertEquals(1, digest.socialMediaItems.size, "Only today's non-declined partnership plan should surface")
        assertEquals(todayCompanyId.toString(), digest.socialMediaItems.first().companyName)
    }
}
