package fr.devlille.partners.connect.companies

import fr.devlille.partners.connect.companies.domain.JobOfferPromotionResponse
import fr.devlille.partners.connect.companies.factories.insertMockCompanyJobOfferPromotion
import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.companies.factories.insertMockedJobOffer
import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.internal.infrastructure.api.PaginatedResponse
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.partnership.factories.insertMockedPartnership
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringPack
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import fr.devlille.partners.connect.users.factories.insertMockedUser
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class CompanyJobOfferRoutesTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `DELETE should be idempotent - deleting already deleted job offer returns 404`() = testApplication {
        val companyId = UUID.randomUUID()
        val jobOfferId = UUID.randomUUID()

        application {
            moduleSharedDb(userId = UUID.randomUUID())
            transaction {
                insertMockedCompany(companyId)
            }
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
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val jobOfferId = UUID.randomUUID()
        val promotionId = UUID.randomUUID()

        application {
            moduleSharedDb(userId = userId)
            transaction {
                insertMockedOrganisationEntity(orgId)
                insertMockedUser(userId)
                insertMockedOrgaPermission(orgId, userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
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
        val listAfterDelete = client.get("/events/$eventId/partnerships/$partnershipId/job-offers")
        assertEquals(HttpStatusCode.OK, listAfterDelete.status)
        val offersAfterDelete = json.decodeFromString<PaginatedResponse<JobOfferPromotionResponse>>(
            listAfterDelete.bodyAsText(),
        )
        assertEquals(0, offersAfterDelete.items.size) // Promotion should be gone
    }
}
