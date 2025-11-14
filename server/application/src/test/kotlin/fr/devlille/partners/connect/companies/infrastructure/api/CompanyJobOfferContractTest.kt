package fr.devlille.partners.connect.companies.infrastructure.api

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.companies.factories.insertMockedJobOffer
import fr.devlille.partners.connect.internal.moduleMocked
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Contract tests for Company job offer API endpoints.
 * Focus: HTTP request/response validation, schema compliance, status codes.
 * Scope: API contract validation without complex business logic testing.
 */
class CompanyJobOfferContractTest {

    @Test
    fun `POST company job offer returns 201 with valid job offer data structure`() = testApplication {
        application { moduleMocked() }

        // Minimal setup using factory function
        val company = insertMockedCompany(name = "Test Company")

        val jobOfferRequest = """
        {
            "title": "Software Engineer",
            "description": "Join our team",
            "location": "Remote",
            "contract_type": "CDI",
            "salary_min": 50000,
            "salary_max": 70000
        }
        """.trimIndent()

        val response = client.post("/companies/${company.id.value}/job-offers") {
            contentType(ContentType.Application.Json)
            setBody(jobOfferRequest)
        }

        // Contract validation: HTTP status code
        assertEquals(HttpStatusCode.Created, response.status)

        // Contract validation: Response structure contains required fields
        val responseBody = response.bodyAsText()
        val jobOfferJson = Json.parseToJsonElement(responseBody).jsonObject

        // Verify response schema structure
        assertTrue(jobOfferJson.containsKey("id"))
        assertTrue(jobOfferJson.containsKey("company_id"))
        assertTrue(jobOfferJson.containsKey("title"))
        assertTrue(jobOfferJson.containsKey("description"))
        assertTrue(jobOfferJson.containsKey("location"))
        assertTrue(jobOfferJson.containsKey("contract_type"))
        assertTrue(jobOfferJson.containsKey("salary_min"))
        assertTrue(jobOfferJson.containsKey("salary_max"))
        assertTrue(jobOfferJson.containsKey("created_at"))
        assertTrue(jobOfferJson.containsKey("updated_at"))

        // Verify company_id consistency
        assertEquals("\"${company.id.value}\"", jobOfferJson["company_id"].toString())
    }

    @Test
    fun `GET company job offers returns 200 with paginated response structure`() = testApplication {
        application { moduleMocked() }

        // Minimal setup using factory functions
        val company = insertMockedCompany(name = "Test Company")
        insertMockedJobOffer(companyId = company.id.value, title = "Job 1")
        insertMockedJobOffer(companyId = company.id.value, title = "Job 2")

        val response = client.get("/companies/${company.id.value}/job-offers")

        // Contract validation: HTTP status code
        assertEquals(HttpStatusCode.OK, response.status)

        // Contract validation: Paginated response structure
        val responseBody = response.bodyAsText()
        val paginatedJson = Json.parseToJsonElement(responseBody).jsonObject

        // Verify paginated response schema
        assertTrue(paginatedJson.containsKey("items"))
        assertTrue(paginatedJson.containsKey("page"))
        assertTrue(paginatedJson.containsKey("page_size"))
        assertTrue(paginatedJson.containsKey("total"))

        // Verify items array contains job offers
        val items = paginatedJson["items"]!!.jsonArray
        assertEquals(2, items.size)

        // Verify job offer items schema structure
        val firstJobOffer = items[0].jsonObject
        assertTrue(firstJobOffer.containsKey("id"))
        assertTrue(firstJobOffer.containsKey("company_id"))
        assertTrue(firstJobOffer.containsKey("title"))
        assertTrue(firstJobOffer.containsKey("description"))
        assertTrue(firstJobOffer.containsKey("location"))
        assertTrue(firstJobOffer.containsKey("contract_type"))
    }

    @Test
    fun `GET company job offer by ID returns 200 with valid job offer data structure`() = testApplication {
        application { moduleMocked() }

        // Minimal setup using factory functions
        val company = insertMockedCompany(name = "Test Company")
        val jobOffer = insertMockedJobOffer(companyId = company.id.value, title = "Test Job")

        val response = client.get("/companies/${company.id.value}/job-offers/${jobOffer.id.value}")

        // Contract validation: HTTP status code
        assertEquals(HttpStatusCode.OK, response.status)

        // Contract validation: Response structure contains required fields
        val responseBody = response.bodyAsText()
        val jobOfferJson = Json.parseToJsonElement(responseBody).jsonObject

        // Verify response schema structure
        assertTrue(jobOfferJson.containsKey("id"))
        assertTrue(jobOfferJson.containsKey("company_id"))
        assertTrue(jobOfferJson.containsKey("title"))
        assertTrue(jobOfferJson.containsKey("description"))
        assertTrue(jobOfferJson.containsKey("location"))
        assertTrue(jobOfferJson.containsKey("contract_type"))

        // Verify IDs consistency
        assertEquals("\"${jobOffer.id.value}\"", jobOfferJson["id"].toString())
        assertEquals("\"${company.id.value}\"", jobOfferJson["company_id"].toString())
    }

    @Test
    fun `POST job offer with non-existent company ID returns 404`() = testApplication {
        application { moduleMocked() }

        val nonExistentId = UUID.randomUUID()
        val jobOfferRequest = """{"title": "Test Job"}"""

        val response = client.post("/companies/$nonExistentId/job-offers") {
            contentType(ContentType.Application.Json)
            setBody(jobOfferRequest)
        }

        // Contract validation: Not found status for non-existent company
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `GET job offer with non-existent ID returns 404`() = testApplication {
        application { moduleMocked() }

        val company = insertMockedCompany()
        val nonExistentJobId = UUID.randomUUID()

        val response = client.get("/companies/${company.id.value}/job-offers/$nonExistentJobId")

        // Contract validation: Not found status for non-existent job offer
        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}
