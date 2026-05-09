package fr.devlille.partners.connect.events

import fr.devlille.partners.connect.agenda.factories.insertMockedSpeaker
import fr.devlille.partners.connect.companies.factories.insertMockCompanyJobOfferPromotion
import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.companies.factories.insertMockedCompanySocial
import fr.devlille.partners.connect.companies.factories.insertMockedJobOffer
import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.internal.infrastructure.db.PromotionStatus
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.partnership.factories.insertMockedBoothActivity
import fr.devlille.partners.connect.partnership.factories.insertMockedCommunicationPlan
import fr.devlille.partners.connect.partnership.factories.insertMockedPartnership
import fr.devlille.partners.connect.partnership.factories.insertMockedPartnershipTicket
import fr.devlille.partners.connect.partnership.factories.insertMockedQandaAnswer
import fr.devlille.partners.connect.partnership.factories.insertMockedQandaQuestion
import fr.devlille.partners.connect.partnership.factories.insertMockedSpeakerPartnership
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import fr.devlille.partners.connect.users.factories.insertMockedUser
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class EventStatsRoutesTest {
    @Test
    @Suppress("LongMethod")
    fun `aggregates per-partner counts and excludes declined partnerships`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val eventSlug = eventId.toString()

        val richCompanyId = UUID.randomUUID()
        val bareCompanyId = UUID.randomUUID()
        val declinedCompanyId = UUID.randomUUID()

        val richPartnershipId = UUID.randomUUID()
        val barePartnershipId = UUID.randomUUID()
        val declinedPartnershipId = UUID.randomUUID()

        val richJobOfferApprovedId = UUID.randomUUID()
        val richJobOfferPendingId = UUID.randomUUID()

        val q1Id = UUID.randomUUID()
        val q2Id = UUID.randomUUID()

        val speakerA = UUID.randomUUID()
        val speakerB = UUID.randomUUID()

        application {
            moduleSharedDb(userId = userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, slug = eventSlug, orgId = orgId)

                insertMockedCompany(richCompanyId)
                insertMockedCompany(bareCompanyId)
                insertMockedCompany(declinedCompanyId)

                insertMockedPartnership(id = richPartnershipId, eventId = eventId, companyId = richCompanyId)
                insertMockedPartnership(id = barePartnershipId, eventId = eventId, companyId = bareCompanyId)
                insertMockedPartnership(
                    id = declinedPartnershipId,
                    eventId = eventId,
                    companyId = declinedCompanyId,
                    declinedAt = LocalDateTime.parse("2024-01-01T00:00:00"),
                )

                // Rich partnership: 2 activities
                insertMockedBoothActivity(partnershipId = richPartnershipId)
                insertMockedBoothActivity(partnershipId = richPartnershipId)

                // Rich partnership: 3 tickets
                repeat(3) { insertMockedPartnershipTicket(partnershipId = richPartnershipId) }

                // Rich partnership: 1 communication plan
                insertMockedCommunicationPlan(eventId = eventId, partnershipId = richPartnershipId)

                // Rich partnership: 2 speakers attached
                insertMockedSpeaker(id = speakerA, eventId = eventId)
                insertMockedSpeaker(id = speakerB, eventId = eventId)
                insertMockedSpeakerPartnership(partnershipId = richPartnershipId, speakerId = speakerA)
                insertMockedSpeakerPartnership(partnershipId = richPartnershipId, speakerId = speakerB)

                // Rich partnership: 2 questions × 3 answers each = 6 answers
                insertMockedQandaQuestion(id = q1Id, partnershipId = richPartnershipId)
                insertMockedQandaQuestion(id = q2Id, partnershipId = richPartnershipId)
                repeat(3) { insertMockedQandaAnswer(questionId = q1Id) }
                repeat(3) { insertMockedQandaAnswer(questionId = q2Id) }

                // Rich partnership: 2 promotions (1 APPROVED + 1 PENDING)
                insertMockedJobOffer(id = richJobOfferApprovedId, companyId = richCompanyId)
                insertMockedJobOffer(id = richJobOfferPendingId, companyId = richCompanyId)
                insertMockCompanyJobOfferPromotion(
                    jobOfferId = richJobOfferApprovedId,
                    partnershipId = richPartnershipId,
                    eventId = eventId,
                    status = PromotionStatus.APPROVED,
                    userReviewId = userId,
                )
                insertMockCompanyJobOfferPromotion(
                    jobOfferId = richJobOfferPendingId,
                    partnershipId = richPartnershipId,
                    eventId = eventId,
                    status = PromotionStatus.PENDING,
                )

                // Rich company: 4 social links
                repeat(4) { insertMockedCompanySocial(companyId = richCompanyId) }

                // Declined partnership has data — should be excluded entirely
                insertMockedBoothActivity(partnershipId = declinedPartnershipId)
                insertMockedCompanySocial(companyId = declinedCompanyId)
            }
        }

        val response = client.get("/orgs/$orgId/events/$eventSlug/stats") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)

        val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        val partners = body["partners"]!!.jsonArray
        assertEquals(2, partners.size, "Declined partnership must be excluded")

        val rich = partners.first {
            it.jsonObject["partnership"]!!.jsonObject["id"]?.jsonPrimitive?.content == richPartnershipId.toString()
        }.jsonObject
        val bare = partners.first {
            it.jsonObject["partnership"]!!.jsonObject["id"]?.jsonPrimitive?.content == barePartnershipId.toString()
        }.jsonObject

        assertEquals(2, rich["activities"]!!.jsonPrimitive.content.toInt())
        assertEquals(3, rich["tickets"]!!.jsonPrimitive.content.toInt())
        assertEquals(1, rich["communication_plan"]!!.jsonPrimitive.content.toInt())
        assertEquals(2, rich["speakers"]!!.jsonPrimitive.content.toInt())
        assertEquals(4, rich["social_links"]!!.jsonPrimitive.content.toInt())

        val richJobOffers = rich["job_offers"]!!.jsonObject
        assertEquals(2, richJobOffers["total"]!!.jsonPrimitive.content.toInt())
        assertEquals(1, richJobOffers["validated"]!!.jsonPrimitive.content.toInt())

        val richQanda = rich["qanda"]!!.jsonObject
        assertEquals(2, richQanda["questions"]!!.jsonPrimitive.content.toInt())
        assertEquals(6, richQanda["answers"]!!.jsonPrimitive.content.toInt())

        // Bare partnership has zeros across the board
        assertEquals(0, bare["activities"]!!.jsonPrimitive.content.toInt())
        assertEquals(0, bare["tickets"]!!.jsonPrimitive.content.toInt())
        assertEquals(0, bare["communication_plan"]!!.jsonPrimitive.content.toInt())
        assertEquals(0, bare["speakers"]!!.jsonPrimitive.content.toInt())
        assertEquals(0, bare["social_links"]!!.jsonPrimitive.content.toInt())
        assertEquals(0, bare["job_offers"]!!.jsonObject["total"]!!.jsonPrimitive.content.toInt())
        assertEquals(0, bare["job_offers"]!!.jsonObject["validated"]!!.jsonPrimitive.content.toInt())
        assertEquals(0, bare["qanda"]!!.jsonObject["questions"]!!.jsonPrimitive.content.toInt())
        assertEquals(0, bare["qanda"]!!.jsonObject["answers"]!!.jsonPrimitive.content.toInt())

        // Sanity: PartnershipItem block is reused
        assertNotNull(rich["partnership"]!!.jsonObject["company_name"]?.jsonPrimitive?.content)
    }
}
