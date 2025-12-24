package fr.devlille.partners.connect.partnership.infrastructure.api

import fr.devlille.partners.connect.companies.factories.insertMockCompanyJobOfferPromotion
import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.companies.factories.insertMockedJobOffer
import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.partnership.domain.DeclineJobOfferRequest
import fr.devlille.partners.connect.partnership.factories.insertMockedPartnership
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringPack
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import fr.devlille.partners.connect.users.factories.insertMockedUser
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class PartnershipJobOfferDeclineRoutePostTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `organizer can decline pending promotion with reason`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val jobOfferId = UUID.randomUUID()
        val promotionId = UUID.randomUUID()
        val declineReason = "The job offer does not match event's technology focus"

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(companyId)
                insertMockedSponsoringPack(packId, eventId)
                insertMockedPartnership(
                    id = partnershipId,
                    eventId = eventId,
                    companyId = companyId,
                    selectedPackId = packId,
                )
                insertMockedJobOffer(companyId = companyId, id = jobOfferId)
                insertMockCompanyJobOfferPromotion(
                    jobOfferId = jobOfferId,
                    partnershipId = partnershipId,
                    eventId = eventId,
                    id = promotionId,
                )
            }
        }

        // Organizer declines the promotion with reason
        val input = json.encodeToString(
            DeclineJobOfferRequest.serializer(),
            DeclineJobOfferRequest(reason = declineReason),
        )
        val declineResponse = client.post(
            "/orgs/$orgId/events/$eventId/partnerships/$partnershipId/job-offers/$promotionId/decline",
        ) {
            header(HttpHeaders.Authorization, "Bearer valid")
            contentType(ContentType.Application.Json)
            setBody(input)
        }
        assertEquals(HttpStatusCode.OK, declineResponse.status)

        val declinedJson = Json.parseToJsonElement(declineResponse.bodyAsText()).jsonObject
        assertEquals("DECLINED", declinedJson["status"]?.jsonPrimitive?.content)
        assertEquals(declineReason, declinedJson["decline_reason"]?.jsonPrimitive?.content)
    }

    @Test
    fun `decline returns 401 when no JWT provided`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val jobOfferId = UUID.randomUUID()
        val promotionId = UUID.randomUUID()
        val declineReason = "The job offer does not match event's technology focus"

        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(companyId)
                insertMockedSponsoringPack(packId, eventId)
                insertMockedPartnership(
                    id = partnershipId,
                    eventId = eventId,
                    companyId = companyId,
                    selectedPackId = packId,
                )
                insertMockedJobOffer(companyId = companyId, id = jobOfferId)
                insertMockCompanyJobOfferPromotion(
                    jobOfferId = jobOfferId,
                    partnershipId = partnershipId,
                    eventId = eventId,
                    id = promotionId,
                )
            }
        }

        // Attempt to decline WITHOUT Authorization header
        val input = json.encodeToString(
            DeclineJobOfferRequest.serializer(),
            DeclineJobOfferRequest(reason = declineReason),
        )
        val declineResponse = client.post(
            "/orgs/$orgId/events/$eventId/partnerships/$partnershipId/job-offers/$promotionId/decline",
        ) {
            contentType(ContentType.Application.Json)
            setBody(input)
        }

        // Should return 401 Unauthorized
        assertEquals(HttpStatusCode.Unauthorized, declineResponse.status)
    }
}
