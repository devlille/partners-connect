package fr.devlille.partners.connect.companies

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.companies.factories.insertMockedJobOffer
import fr.devlille.partners.connect.internal.moduleMocked
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CompanyJobOfferRoutesGetTest {
    @Test
    fun `GET job offer by ID should return job offer with 200`() = testApplication {
        val companyId = UUID.randomUUID()
        val jobOfferId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedCompany(companyId)
            insertMockedJobOffer(companyId, jobOfferId)
        }

        val response = client.get("/companies/$companyId/job-offers/$jobOfferId")

        assertEquals(HttpStatusCode.OK, response.status)

        val jobOffer = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        assertTrue(jobOffer.containsKey("id"))
        assertTrue(jobOffer.containsKey("company_id"))
        assertTrue(jobOffer.containsKey("url"))
        assertTrue(jobOffer.containsKey("title"))
        assertTrue(jobOffer.containsKey("location"))
        assertTrue(jobOffer.containsKey("publication_date"))
        assertTrue(jobOffer.containsKey("created_at"))
        assertTrue(jobOffer.containsKey("updated_at"))

        assertEquals(jobOfferId.toString(), jobOffer["id"]?.jsonPrimitive?.content)
        assertEquals(companyId.toString(), jobOffer["company_id"]?.jsonPrimitive?.content)
    }

    @Test
    fun `GET non-existent job offer should return 404`() = testApplication {
        val companyId = UUID.randomUUID()
        val nonExistentJobOfferId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedCompany(companyId)
        }

        val response = client.get("/companies/$companyId/job-offers/$nonExistentJobOfferId")

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `GET job offer for non-existent company should return 404`() = testApplication {
        val nonExistentCompanyId = UUID.randomUUID()
        val jobOfferId = UUID.randomUUID()

        application {
            moduleMocked()
            // Do not insert company
        }

        val response = client.get("/companies/$nonExistentCompanyId/job-offers/$jobOfferId")

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `GET job offer with invalid UUID should return 400`() = testApplication {
        val companyId = UUID.randomUUID()
        val invalidJobOfferId = "invalid-uuid"

        application {
            moduleMocked()
            insertMockedCompany(companyId)
        }

        val response = client.get("/companies/$companyId/job-offers/$invalidJobOfferId")

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `GET job offer from different company should return 404`() = testApplication {
        val companyId1 = UUID.randomUUID()
        val companyId2 = UUID.randomUUID()
        val jobOfferId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedCompany(companyId1)
            insertMockedCompany(companyId2)
            insertMockedJobOffer(companyId1, jobOfferId)
        }

        // Try to access job offer from companyId1 via companyId2's endpoint
        val response = client.get("/companies/$companyId2/job-offers/$jobOfferId")

        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}
