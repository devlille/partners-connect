package fr.devlille.partners.connect.companies

import fr.devlille.partners.connect.companies.domain.JobOfferPromotionResponse
import fr.devlille.partners.connect.companies.factories.insertMockCompanyJobOfferPromotion
import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.companies.factories.insertMockedJobOffer
import fr.devlille.partners.connect.events.factories.insertMockedEventWithOrga
import fr.devlille.partners.connect.internal.infrastructure.api.PaginatedResponse
import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.partnership.factories.insertMockedPartnership
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringPack
import fr.devlille.partners.connect.users.factories.insertMockedAdminUser
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
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

class CompanyJobOfferRoutesDeleteTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `DELETE job offer should remove job offer and return 204`() = testApplication {
        val companyId = UUID.randomUUID()
        val jobOfferId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedCompany(companyId)
            insertMockedJobOffer(companyId, jobOfferId)
        }

        val response = client.delete("/companies/$companyId/job-offers/$jobOfferId")

        assertEquals(HttpStatusCode.NoContent, response.status)
    }

    @Test
    fun `DELETE non-existent job offer should return 404`() = testApplication {
        val companyId = UUID.randomUUID()
        val nonExistentJobOfferId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedCompany(companyId)
        }

        val response = client.delete("/companies/$companyId/job-offers/$nonExistentJobOfferId")

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `DELETE job offer for non-existent company should return 404`() = testApplication {
        val nonExistentCompanyId = UUID.randomUUID()
        val jobOfferId = UUID.randomUUID()

        application {
            moduleMocked()
            // Do not insert company
        }

        val response = client.delete("/companies/$nonExistentCompanyId/job-offers/$jobOfferId")

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `DELETE job offer with invalid UUID should return 400`() = testApplication {
        val companyId = UUID.randomUUID()
        val invalidJobOfferId = "invalid-uuid"

        application {
            moduleMocked()
            insertMockedCompany(companyId)
        }

        val response = client.delete("/companies/$companyId/job-offers/$invalidJobOfferId")

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `DELETE job offer from different company should return 404`() = testApplication {
        val companyId1 = UUID.randomUUID()
        val companyId2 = UUID.randomUUID()
        val jobOfferId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedCompany(companyId1)
            insertMockedCompany(companyId2)
            insertMockedJobOffer(companyId1, jobOfferId)
        }

        // Try to delete job offer from companyId1 via companyId2's endpoint
        val response = client.delete("/companies/$companyId2/job-offers/$jobOfferId")

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `DELETE should be idempotent - deleting already deleted job offer returns 404`() = testApplication {
        val companyId = UUID.randomUUID()
        val jobOfferId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedCompany(companyId)
        }

        // First deletion attempt
        val firstResponse = client.delete("/companies/$companyId/job-offers/$jobOfferId")
        assertEquals(HttpStatusCode.NotFound, firstResponse.status) // Will be 404 until implementation

        // Second deletion attempt should also return 404
        val secondResponse = client.delete("/companies/$companyId/job-offers/$jobOfferId")
        assertEquals(HttpStatusCode.NotFound, secondResponse.status)
    }

    @Test
    fun `DELETE job offer cascades deletion of promotions`() = testApplication {
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

        // Delete the job offer
        val deleteResponse = client.delete("/companies/$companyId/job-offers/$jobOfferId") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }
        assertEquals(HttpStatusCode.NoContent, deleteResponse.status)

        // Verify promotion was CASCADE deleted
        val listAfterDelete = client.get("/events/$eventSlug/partnerships/$partnershipId/job-offers")
        assertEquals(HttpStatusCode.OK, listAfterDelete.status)
        val offersAfterDelete = json.decodeFromString<PaginatedResponse<JobOfferPromotionResponse>>(
            listAfterDelete.bodyAsText(),
        )
        assertEquals(0, offersAfterDelete.items.size) // Promotion should be gone
    }
}
