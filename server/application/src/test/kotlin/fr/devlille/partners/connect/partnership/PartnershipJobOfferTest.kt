package fr.devlille.partners.connect.partnership

import fr.devlille.partners.connect.companies.factories.insertMockCompanyJobOfferPromotion
import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.companies.factories.insertMockedJobOffer
import fr.devlille.partners.connect.events.factories.insertMockedEventWithOrga
import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.partnership.domain.DeclineJobOfferRequest
import fr.devlille.partners.connect.partnership.factories.insertMockedPartnership
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringPack
import fr.devlille.partners.connect.users.factories.insertMockedAdminUser
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
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

class PartnershipJobOfferTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `organizer can approve pending promotion`() = testApplication {
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val jobOfferId = UUID.randomUUID()
        val promotionId = UUID.randomUUID()
        val eventSlug = "devconf-2025"
        val orgSlug = "devconf"

        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        application {
            moduleMocked()
            transaction {
                val org = insertMockedOrganisationEntity(name = orgSlug)
                insertMockedEventWithOrga(
                    id = eventId,
                    slug = eventSlug,
                    startTime = "${now.year + 1}-12-01T00:00:00",
                    endTime = "${now.year + 1}-12-31T23:59:59",
                )
                insertMockedOrgaPermission(
                    orgId = org.id.value,
                    user = insertMockedAdminUser(),
                )
                insertMockedCompany(companyId)
                insertMockedSponsoringPack(packId, eventId)
                insertMockedPartnership(
                    id = partnershipId,
                    eventId = eventId,
                    companyId = companyId,
                    selectedPackId = packId,
                )
                insertMockedJobOffer(
                    companyId = companyId,
                    id = jobOfferId,
                )
                insertMockCompanyJobOfferPromotion(
                    jobOfferId = jobOfferId,
                    partnershipId = partnershipId,
                    eventId = eventId,
                    id = promotionId,
                )
            }
        }

        val approveResponse = client.post(
            "/orgs/$orgSlug/events/$eventSlug/partnerships/$partnershipId/job-offers/$promotionId/approve",
        ) {
            header(HttpHeaders.Authorization, "Bearer valid")
            contentType(ContentType.Application.Json)
        }
        assertEquals(HttpStatusCode.OK, approveResponse.status)

        val approvedJson = Json.parseToJsonElement(approveResponse.bodyAsText()).jsonObject
        assertEquals("APPROVED", approvedJson["status"]?.jsonPrimitive?.content)
    }

    @Test
    fun `organizer can decline pending promotion with reason`() = testApplication {
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val jobOfferId = UUID.randomUUID()
        val promotionId = UUID.randomUUID()
        val eventSlug = "devconf-2025"
        val orgSlug = "devconf"
        val declineReason = "The job offer does not match event's technology focus"

        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        application {
            moduleMocked()
            transaction {
                val org = insertMockedOrganisationEntity(name = orgSlug)
                insertMockedEventWithOrga(
                    id = eventId,
                    slug = eventSlug,
                    startTime = "${now.year + 1}-12-01T00:00:00",
                    endTime = "${now.year + 1}-12-31T23:59:59",
                )
                insertMockedOrgaPermission(orgId = org.id.value, user = insertMockedAdminUser())
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
            "/orgs/$orgSlug/events/$eventSlug/partnerships/$partnershipId/job-offers/$promotionId/decline",
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
    fun `approve returns 401 when no JWT provided`() = testApplication {
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val jobOfferId = UUID.randomUUID()
        val promotionId = UUID.randomUUID()
        val eventSlug = "devconf-2025"
        val orgSlug = "devconf"

        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        application {
            moduleMocked()
            transaction {
                val org = insertMockedOrganisationEntity(name = orgSlug)
                insertMockedEventWithOrga(
                    id = eventId,
                    slug = eventSlug,
                    startTime = "${now.year + 1}-12-01T00:00:00",
                    endTime = "${now.year + 1}-12-31T23:59:59",
                )
                insertMockedOrgaPermission(
                    orgId = org.id.value,
                    user = insertMockedAdminUser(),
                )
                insertMockedCompany(companyId)
                insertMockedSponsoringPack(packId, eventId)
                insertMockedPartnership(
                    id = partnershipId,
                    eventId = eventId,
                    companyId = companyId,
                    selectedPackId = packId,
                )
                insertMockedJobOffer(
                    companyId = companyId,
                    id = jobOfferId,
                )
                insertMockCompanyJobOfferPromotion(
                    jobOfferId = jobOfferId,
                    partnershipId = partnershipId,
                    eventId = eventId,
                    id = promotionId,
                )
            }
        }

        // Attempt to approve WITHOUT Authorization header
        val approveResponse = client.post(
            "/orgs/$orgSlug/events/$eventSlug/partnerships/$partnershipId/job-offers/$promotionId/approve",
        ) {
            // No Authorization header
            contentType(ContentType.Application.Json)
        }

        // Should return 401 Unauthorized
        assertEquals(HttpStatusCode.Unauthorized, approveResponse.status)
    }

    @Test
    fun `decline returns 401 when no JWT provided`() = testApplication {
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val jobOfferId = UUID.randomUUID()
        val promotionId = UUID.randomUUID()
        val eventSlug = "devconf-2025"
        val orgSlug = "devconf"
        val declineReason = "The job offer does not match event's technology focus"

        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        application {
            moduleMocked()
            transaction {
                val org = insertMockedOrganisationEntity(name = orgSlug)
                insertMockedEventWithOrga(
                    id = eventId,
                    slug = eventSlug,
                    startTime = "${now.year + 1}-12-01T00:00:00",
                    endTime = "${now.year + 1}-12-31T23:59:59",
                )
                insertMockedOrgaPermission(
                    orgId = org.id.value,
                    user = insertMockedAdminUser(),
                )
                insertMockedCompany(companyId)
                insertMockedSponsoringPack(packId, eventId)
                insertMockedPartnership(
                    id = partnershipId,
                    eventId = eventId,
                    companyId = companyId,
                    selectedPackId = packId,
                )
                insertMockedJobOffer(
                    companyId = companyId,
                    id = jobOfferId,
                )
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
            "/orgs/$orgSlug/events/$eventSlug/partnerships/$partnershipId/job-offers/$promotionId/decline",
        ) {
            contentType(ContentType.Application.Json)
            setBody(input)
        }

        // Should return 401 Unauthorized
        assertEquals(HttpStatusCode.Unauthorized, declineResponse.status)
    }
}
