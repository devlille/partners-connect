package fr.devlille.partners.connect.companies.infrastructure.api

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.internal.moduleMocked
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Contract tests for Company listing API endpoint.
 * Focus: HTTP request/response validation, schema compliance, status codes.
 * Scope: API contract validation without complex business logic testing.
 */
class CompanyListContractTest {

    @Test
    fun `GET companies returns 200 with empty paginated response structure`() = testApplication {
        application { moduleMocked() }

        val response = client.get("/companies")

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

        // Verify items is array
        val items = paginatedJson["items"]!!.jsonArray
        assertEquals(0, items.size)

        // Verify pagination metadata types
        assertEquals(1, paginatedJson["page"]!!.toString().toInt())
        assertEquals(20, paginatedJson["page_size"]!!.toString().toInt())
        assertEquals(0, paginatedJson["total"]!!.toString().toInt())
    }

    @Test
    fun `GET companies returns 200 with populated paginated response structure`() = testApplication {
        application { moduleMocked() }

        // Minimal setup using factory functions
        insertMockedCompany(name = "Company 1")
        insertMockedCompany(name = "Company 2")

        val response = client.get("/companies")

        // Contract validation: HTTP status code
        assertEquals(HttpStatusCode.OK, response.status)

        // Contract validation: Response structure with data
        val responseBody = response.bodyAsText()
        val paginatedJson = Json.parseToJsonElement(responseBody).jsonObject

        // Verify paginated response schema
        assertTrue(paginatedJson.containsKey("items"))
        assertTrue(paginatedJson.containsKey("page"))
        assertTrue(paginatedJson.containsKey("page_size"))
        assertTrue(paginatedJson.containsKey("total"))

        // Verify items array contains companies
        val items = paginatedJson["items"]!!.jsonArray
        assertEquals(2, items.size)

        // Verify company items schema structure
        val firstCompany = items[0].jsonObject
        assertTrue(firstCompany.containsKey("id"))
        assertTrue(firstCompany.containsKey("name"))
        assertTrue(firstCompany.containsKey("status"))
        assertTrue(firstCompany.containsKey("created_at"))

        // Verify pagination metadata with data
        assertEquals(1, paginatedJson["page"]!!.toString().toInt())
        assertEquals(20, paginatedJson["page_size"]!!.toString().toInt())
        assertEquals(2, paginatedJson["total"]!!.toString().toInt())
    }

    @Test
    fun `GET companies with page parameter returns 200 with correct pagination`() = testApplication {
        application { moduleMocked() }

        val response = client.get("/companies?page=2")

        // Contract validation: HTTP status code
        assertEquals(HttpStatusCode.OK, response.status)

        // Contract validation: Pagination parameter handling
        val responseBody = response.bodyAsText()
        val paginatedJson = Json.parseToJsonElement(responseBody).jsonObject

        assertEquals(2, paginatedJson["page"]!!.toString().toInt())
    }

    @Test
    fun `GET companies with invalid page parameter returns 400`() = testApplication {
        application { moduleMocked() }

        val response = client.get("/companies?page=invalid")

        // Contract validation: Bad request status for invalid pagination
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }
}
