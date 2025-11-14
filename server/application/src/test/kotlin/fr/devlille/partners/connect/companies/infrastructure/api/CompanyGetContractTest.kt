package fr.devlille.partners.connect.companies.infrastructure.api

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.internal.moduleMocked
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Contract tests for Company retrieval API endpoint.
 * Focus: HTTP request/response validation, schema compliance, status codes.
 * Scope: API contract validation without complex business logic testing.
 */
class CompanyGetContractTest {

    @Test
    fun `GET company by ID returns 200 with valid company data structure`() = testApplication {
        application { moduleMocked() }

        // Minimal setup using factory function
        val company = insertMockedCompany(name = "Test Company")

        val response = client.get("/companies/${company.id.value}")

        // Contract validation: HTTP status code
        assertEquals(HttpStatusCode.OK, response.status)

        // Contract validation: Response structure contains required fields
        val responseBody = response.bodyAsText()
        val companyJson = Json.parseToJsonElement(responseBody).jsonObject

        // Verify response schema structure
        assertTrue(companyJson.containsKey("id"))
        assertTrue(companyJson.containsKey("name"))
        assertTrue(companyJson.containsKey("address"))
        assertTrue(companyJson.containsKey("city"))
        assertTrue(companyJson.containsKey("zip_code"))
        assertTrue(companyJson.containsKey("country"))
        assertTrue(companyJson.containsKey("siret"))
        assertTrue(companyJson.containsKey("vat"))
        assertTrue(companyJson.containsKey("description"))
        assertTrue(companyJson.containsKey("site_url"))
        assertTrue(companyJson.containsKey("status"))
        assertTrue(companyJson.containsKey("logo_url_original"))
        assertTrue(companyJson.containsKey("logo_url_1000"))
        assertTrue(companyJson.containsKey("logo_url_500"))
        assertTrue(companyJson.containsKey("logo_url_250"))
        assertTrue(companyJson.containsKey("created_at"))
        assertTrue(companyJson.containsKey("updated_at"))

        // Verify data types (basic validation)
        assertEquals("\"${company.id.value}\"", companyJson["id"].toString())
        assertEquals("\"Test Company\"", companyJson["name"].toString())
    }

    @Test
    fun `GET company by non-existent ID returns 404`() = testApplication {
        application { moduleMocked() }

        val nonExistentId = UUID.randomUUID()
        val response = client.get("/companies/$nonExistentId")

        // Contract validation: Not found status for non-existent resource
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `GET company with invalid ID format returns 400`() = testApplication {
        application { moduleMocked() }

        val invalidId = "not-a-uuid"
        val response = client.get("/companies/$invalidId")

        // Contract validation: Bad request status for invalid ID format
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }
}
