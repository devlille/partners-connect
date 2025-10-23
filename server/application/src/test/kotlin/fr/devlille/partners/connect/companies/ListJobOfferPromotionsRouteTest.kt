package fr.devlille.partners.connect.companies

import fr.devlille.partners.connect.companies.domain.JobOfferPromotionResponse
import fr.devlille.partners.connect.companies.factories.insertMockCompanyJobOfferPromotion
import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.companies.factories.insertMockedJobOffer
import fr.devlille.partners.connect.events.factories.insertMockedEventWithOrga
import fr.devlille.partners.connect.internal.infrastructure.api.PaginatedResponse
import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.partnership.factories.insertMockedPartnership
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringPack
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Contract tests for GET /companies/{companyId}/job-offers/{jobOfferId}/promotions endpoint.
 * Tests retrieving promotions for a specific job offer.
 */
class ListJobOfferPromotionsRouteTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `GET promotions returns 200 with empty list when no promotions exist`() = testApplication {
        val companyId = UUID.randomUUID()
        val jobOfferId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedCompany(companyId)
            insertMockedJobOffer(companyId = companyId, id = jobOfferId)
        }

        val response = client.get("/companies/$companyId/job-offers/$jobOfferId/promotions")

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        // Expected pagination response format
        assertTrue(body.contains("\"items\":[]") || body.contains("[]"))
    }

    @Test
    fun `GET promotions returns 200 with paginated results`() = testApplication {
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

        val response = client.get("/companies/$companyId/job-offers/$jobOfferId/promotions?page=1&page_size=10")

        assertEquals(HttpStatusCode.OK, response.status)
        val body = json.decodeFromString(
            PaginatedResponse.serializer(JobOfferPromotionResponse.serializer()),
            response.bodyAsText(),
        )
        assertEquals(1, body.items.size)
    }

    @Test
    fun `GET promotions returns 200 with partnership_id filter applied`() = testApplication {
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

        val response = client.get(
            "/companies/$companyId/job-offers/$jobOfferId/promotions?partnership_id=$partnershipId",
        )

        assertEquals(HttpStatusCode.OK, response.status)
        val body = json.decodeFromString(
            PaginatedResponse.serializer(JobOfferPromotionResponse.serializer()),
            response.bodyAsText(),
        )
        assertEquals(1, body.items.size)
    }

    @Test
    fun `GET promotions returns 404 when job offer does not exist`() = testApplication {
        val companyId = UUID.randomUUID()
        val nonExistentJobOfferId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedCompany(companyId)
        }

        val response = client.get("/companies/$companyId/job-offers/$nonExistentJobOfferId/promotions")

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `GET promotions returns 404 when company does not exist`() = testApplication {
        val nonExistentCompanyId = UUID.randomUUID()
        val jobOfferId = UUID.randomUUID()

        application {
            moduleMocked()
        }

        val response = client.get("/companies/$nonExistentCompanyId/job-offers/$jobOfferId/promotions")

        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}
