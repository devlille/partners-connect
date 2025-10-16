package fr.devlille.partners.connect.companies

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.companies.factories.insertMockedJobOffer
import fr.devlille.partners.connect.internal.moduleMocked
import io.ktor.client.request.delete
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class CompanyJobOfferRoutesDeleteTest {
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
}
