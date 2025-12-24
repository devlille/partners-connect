package fr.devlille.partners.connect.companies.infrastructure.api

import fr.devlille.partners.connect.companies.domain.PromoteJobOfferRequest
import fr.devlille.partners.connect.companies.factories.insertMockCompanyJobOfferPromotion
import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.companies.factories.insertMockedJobOffer
import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.events.factories.insertMockedPastEvent
import fr.devlille.partners.connect.internal.infrastructure.db.PromotionStatus
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.partnership.factories.insertMockedPartnership
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringPack
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Contract tests for POST /companies/{companyId}/partnerships/{partnershipId}/promote endpoint.
 * Tests the job offer promotion workflow for companies.
 */
class PromoteJobOfferRoutePostTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `POST promote returns 201 when job offer is successfully promoted`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val jobOfferId = UUID.randomUUID()

        application {
            moduleSharedDb(userId = UUID.randomUUID())
            transaction {
                insertMockedOrganisationEntity(orgId)
                insertMockedFutureEvent(id = eventId, orgId = orgId)
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
    fun `POST promote return 201 when job offer is re-promoted and status resets to pending`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val jobOfferId = UUID.randomUUID()
        val promotionId = UUID.randomUUID()
        val declineReason = "The job offer does not match event's technology focus"

        application {
            moduleSharedDb(userId = UUID.randomUUID())
            transaction {
                insertMockedOrganisationEntity(orgId)
                insertMockedFutureEvent(id = eventId, orgId = orgId)
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

    @Test
    fun `POST promote returns 400 when job_offer_id is missing`() = testApplication {
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleSharedDb(userId = UUID.randomUUID())
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
            moduleSharedDb(userId = UUID.randomUUID())
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
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val jobOfferId = UUID.randomUUID()

        application {
            moduleSharedDb(userId = UUID.randomUUID())
            transaction {
                insertMockedOrganisationEntity(orgId)
                insertMockedPastEvent(id = eventId, orgId = orgId)
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
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val nonExistentJobOfferId = UUID.randomUUID()

        application {
            moduleSharedDb(userId = UUID.randomUUID())
            transaction {
                insertMockedOrganisationEntity(orgId)
                insertMockedFutureEvent(id = eventId, orgId = orgId)
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
            moduleSharedDb(userId = UUID.randomUUID())
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
    fun `POST promote returns 409 when promotion already exists with status approved`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val jobOfferId = UUID.randomUUID()

        application {
            moduleSharedDb(userId = UUID.randomUUID())
            transaction {
                insertMockedOrganisationEntity(orgId)
                insertMockedFutureEvent(id = eventId, orgId = orgId)
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
}
