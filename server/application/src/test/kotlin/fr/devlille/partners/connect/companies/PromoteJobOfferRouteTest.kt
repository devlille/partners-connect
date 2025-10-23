package fr.devlille.partners.connect.companies

import fr.devlille.partners.connect.companies.domain.PromoteJobOfferRequest
import fr.devlille.partners.connect.companies.factories.insertMockCompanyJobOfferPromotion
import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.companies.factories.insertMockedJobOffer
import fr.devlille.partners.connect.events.factories.insertMockedEventWithOrga
import fr.devlille.partners.connect.internal.infrastructure.db.PromotionStatus
import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.partnership.factories.insertMockedPartnership
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringPack
import fr.devlille.partners.connect.users.factories.insertMockedAdminUser
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Contract tests for POST /companies/{companyId}/partnerships/{partnershipId}/promote endpoint.
 * Tests the job offer promotion workflow for companies.
 */
class PromoteJobOfferRouteTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `POST promote returns 201 when job offer is successfully promoted`() = testApplication {
        val companyId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val jobOfferId = UUID.randomUUID()

        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        application {
            moduleMocked()
            transaction {
                insertMockedEventWithOrga(
                    id = eventId,
                    startTime = "${now.year + 1}-12-01T00:00:00",
                    endTime = "${now.year + 1}-12-31T23:59:59",
                )
                insertMockedCompany(companyId)
                insertMockedSponsoringPack(packId, eventId)
                insertMockedPartnership(
                    id = partnershipId,
                    eventId = eventId,
                    companyId = companyId,
                    selectedPackId = packId,
                )
                insertMockedJobOffer(companyId = companyId, id = jobOfferId)
            }
        }

        val input = PromoteJobOfferRequest(jobOfferId.toString())
        val response = client.post("/companies/$companyId/partnerships/$partnershipId/promote") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(PromoteJobOfferRequest.serializer(), input))
        }

        assertEquals(HttpStatusCode.Created, response.status)
    }

    @Test
    fun `POST promote returns 400 when job_offer_id is missing`() = testApplication {
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleMocked()
        }

        val response = client.post("/companies/$companyId/partnerships/$partnershipId/promote") {
            contentType(ContentType.Application.Json)
            setBody("""{}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `POST promote returns 400 when job_offer_id is not a valid UUID`() = testApplication {
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleMocked()
        }

        val input = PromoteJobOfferRequest("invalid-uuid")
        val response = client.post("/companies/$companyId/partnerships/$partnershipId/promote") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(PromoteJobOfferRequest.serializer(), input))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `POST promote returns 403 when event has ended`() = testApplication {
        val companyId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val jobOfferId = UUID.randomUUID()

        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        application {
            moduleMocked()
            transaction {
                // Create event that has already ended
                insertMockedEventWithOrga(
                    id = eventId,
                    startTime = "${now.year - 1}-01-01T00:00:00",
                    endTime = "${now.year - 1}-01-02T00:00:00",
                )
                insertMockedCompany(companyId)
                insertMockedSponsoringPack(packId, eventId)
                insertMockedPartnership(
                    id = partnershipId,
                    eventId = eventId,
                    companyId = companyId,
                    selectedPackId = packId,
                )
                insertMockedJobOffer(companyId = companyId, id = jobOfferId)
            }
        }

        val input = PromoteJobOfferRequest(jobOfferId.toString())
        val response = client.post("/companies/$companyId/partnerships/$partnershipId/promote") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(PromoteJobOfferRequest.serializer(), input))
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `POST promote returns 404 when job offer does not exist`() = testApplication {
        val companyId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val nonExistentJobOfferId = UUID.randomUUID()

        application {
            moduleMocked()
            transaction {
                insertMockedEventWithOrga(id = eventId)
                insertMockedCompany(companyId)
                insertMockedSponsoringPack(packId, eventId)
                insertMockedPartnership(
                    id = partnershipId,
                    eventId = eventId,
                    companyId = companyId,
                    selectedPackId = packId,
                )
            }
        }

        val input = PromoteJobOfferRequest(nonExistentJobOfferId.toString())
        val response = client.post("/companies/$companyId/partnerships/$partnershipId/promote") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(PromoteJobOfferRequest.serializer(), input))
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `POST promote returns 404 when partnership does not exist`() = testApplication {
        val companyId = UUID.randomUUID()
        val nonExistentPartnershipId = UUID.randomUUID()
        val jobOfferId = UUID.randomUUID()

        application {
            moduleMocked()
            transaction {
                insertMockedCompany(companyId)
                insertMockedJobOffer(companyId = companyId, id = jobOfferId)
            }
        }

        val input = PromoteJobOfferRequest(jobOfferId.toString())
        val response = client.post("/companies/$companyId/partnerships/$nonExistentPartnershipId/promote") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(PromoteJobOfferRequest.serializer(), input))
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `POST promote returns 409 when promotion already exists with status pending`() = testApplication {
        val companyId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val jobOfferId = UUID.randomUUID()

        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        application {
            moduleMocked()
            transaction {
                insertMockedEventWithOrga(
                    id = eventId,
                    startTime = "${now.year + 1}-12-01T00:00:00",
                    endTime = "${now.year + 1}-12-31T23:59:59",
                )
                insertMockedCompany(companyId)
                insertMockedSponsoringPack(packId, eventId)
                insertMockedPartnership(
                    id = partnershipId,
                    eventId = eventId,
                    companyId = companyId,
                    selectedPackId = packId,
                )
                insertMockedJobOffer(companyId = companyId, id = jobOfferId)
            }
        }

        // First promotion
        val input = PromoteJobOfferRequest(jobOfferId.toString())
        client.post("/companies/$companyId/partnerships/$partnershipId/promote") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(PromoteJobOfferRequest.serializer(), input))
        }

        // Attempt duplicate promotion
        val response = client.post("/companies/$companyId/partnerships/$partnershipId/promote") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(PromoteJobOfferRequest.serializer(), input))
        }

        assertEquals(HttpStatusCode.Conflict, response.status)
    }

    @Test
    fun `POST promote returns 409 when promotion already exists with status approved`() = testApplication {
        val companyId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val jobOfferId = UUID.randomUUID()

        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        application {
            moduleMocked()
            transaction {
                insertMockedEventWithOrga(
                    id = eventId,
                    startTime = "${now.year + 1}-12-01T00:00:00",
                    endTime = "${now.year + 1}-12-31T23:59:59",
                )
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
                )
            }
        }

        val input = PromoteJobOfferRequest(jobOfferId.toString())
        val response = client.post("/companies/$companyId/partnerships/$partnershipId/promote") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(PromoteJobOfferRequest.serializer(), input))
        }

        assertEquals(HttpStatusCode.Conflict, response.status)
    }

    @Test
    fun `POST promote return 201 when job offer is re-promoted and status resets to pending`() = testApplication {
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
                    status = PromotionStatus.DECLINED,
                    declineReason = declineReason,
                )
            }
        }

        val input = PromoteJobOfferRequest(jobOfferId.toString())
        val promoteResponse2 = client.post("/companies/$companyId/partnerships/$partnershipId/promote") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(PromoteJobOfferRequest.serializer(), input))
        }
        assertEquals(HttpStatusCode.Created, promoteResponse2.status)
    }
}
