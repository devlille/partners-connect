package fr.devlille.partners.connect.companies.infrastructure.api

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.companies.factories.insertMockedJobOffer
import fr.devlille.partners.connect.internal.moduleSharedDb
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CompanyJobOfferRouteListTest {
    @Test
    fun `GET job offers should return paginated list with 200`() = testApplication {
        val companyId = UUID.randomUUID()

        application {
            moduleSharedDb(userId = UUID.randomUUID())
            transaction {
                insertMockedCompany(companyId)
                insertMockedJobOffer(companyId)
            }
        }

        val response = client.get("/companies/$companyId/job-offers")

        assertEquals(HttpStatusCode.OK, response.status)

        val responseMap = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        // Verify response structure
        assertTrue(responseMap.containsKey("items"))
        assertTrue(responseMap.containsKey("page"))
        assertTrue(responseMap.containsKey("page_size"))
        assertTrue(responseMap.containsKey("total"))
    }

    @Test
    fun `GET job offers with pagination parameters should return correct page`() = testApplication {
        val companyId = UUID.randomUUID()

        application {
            moduleSharedDb(userId = UUID.randomUUID())
            transaction {
                insertMockedCompany(companyId)
            }
        }

        val response = client.get("/companies/$companyId/job-offers?page=2&page_size=5")

        assertEquals(HttpStatusCode.OK, response.status)

        val responseMap = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        // Verify pagination parameters are respected
        assertEquals(2, responseMap["page"]?.jsonPrimitive?.content?.toInt())
        assertEquals(5, responseMap["page_size"]?.jsonPrimitive?.content?.toInt())
    }

    @Test
    fun `GET job offers for non-existent company should return 404`() = testApplication {
        val nonExistentCompanyId = UUID.randomUUID()

        application {
            moduleSharedDb(userId = UUID.randomUUID())
            // Do not insert company
        }

        val response = client.get("/companies/$nonExistentCompanyId/job-offers")

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `GET job offers should handle empty results correctly`() = testApplication {
        val companyId = UUID.randomUUID()

        application {
            moduleSharedDb(userId = UUID.randomUUID())
            transaction {
                insertMockedCompany(companyId)
                // No job offers inserted - empty result set
            }
        }

        val response = client.get("/companies/$companyId/job-offers")

        assertEquals(HttpStatusCode.OK, response.status)

        val responseMap = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        val items = responseMap["items"]?.jsonArray

        assertEquals(0, items!!.size)
        assertEquals(0, responseMap["total"]?.jsonPrimitive?.content?.toInt())
    }
}
