package fr.devlille.partners.connect.companies.infrastructure.api

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.internal.moduleMocked
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Contract tests for Company update API endpoint.
 * Focus: HTTP request/response validation, schema compliance, status codes.
 * Scope: API contract validation without complex business logic testing.
 */
class CompanyUpdateContractTest {

    @Test
    fun `PUT company returns 200 with valid update data structure`() = testApplication {
        application { moduleMocked() }

        // Minimal setup using factory function
        val company = insertMockedCompany(name = "Original Company")

        val updateRequest = """
        {
            "name": "Updated Company",
            "description": "Updated description",
            "status": "ACTIVE"
        }
        """.trimIndent()

        val response = client.put("/companies/${company.id.value}") {
            contentType(ContentType.Application.Json)
            setBody(updateRequest)
        }

        // Contract validation: HTTP status code
        assertEquals(HttpStatusCode.OK, response.status)

        // Contract validation: Response structure contains updated fields
        val responseBody = response.bodyAsText()
        val companyJson = Json.parseToJsonElement(responseBody).jsonObject

        // Verify response schema structure
        assertTrue(companyJson.containsKey("id"))
        assertTrue(companyJson.containsKey("name"))
        assertTrue(companyJson.containsKey("description"))
        assertTrue(companyJson.containsKey("status"))
        assertTrue(companyJson.containsKey("updated_at"))

        // Verify ID consistency
        assertEquals("\"${company.id.value}\"", companyJson["id"].toString())
    }

    @Test
    fun `PUT company with non-existent ID returns 404`() = testApplication {
        application { moduleMocked() }

        val nonExistentId = UUID.randomUUID()
        val updateRequest = """{"name": "Updated Name"}"""

        val response = client.put("/companies/$nonExistentId") {
            contentType(ContentType.Application.Json)
            setBody(updateRequest)
        }

        // Contract validation: Not found status for non-existent resource
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `PUT company with invalid ID format returns 400`() = testApplication {
        application { moduleMocked() }

        val invalidId = "not-a-uuid"
        val updateRequest = """{"name": "Updated Name"}"""

        val response = client.put("/companies/$invalidId") {
            contentType(ContentType.Application.Json)
            setBody(updateRequest)
        }

        // Contract validation: Bad request status for invalid ID format
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `PUT company with invalid JSON returns 400`() = testApplication {
        application { moduleMocked() }

        val company = insertMockedCompany()
        val invalidRequest = """{"invalid": json structure}"""

        val response = client.put("/companies/${company.id.value}") {
            contentType(ContentType.Application.Json)
            setBody(invalidRequest)
        }

        // Contract validation: Bad request status for invalid JSON
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }
}
