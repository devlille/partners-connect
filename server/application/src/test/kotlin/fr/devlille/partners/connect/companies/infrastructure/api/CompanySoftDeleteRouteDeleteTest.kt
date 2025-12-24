package fr.devlille.partners.connect.companies.infrastructure.api

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.internal.moduleSharedDb
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class CompanySoftDeleteRouteDeleteTest {
    @Test
    fun `DELETE should soft delete company and return 204`() = testApplication {
        val companyId = UUID.randomUUID()

        application {
            moduleSharedDb(userId = UUID.randomUUID())
            transaction {
                insertMockedCompany(companyId, name = "Company To Delete")
            }
        }

        val response = client.delete("/companies/$companyId")

        assertEquals(HttpStatusCode.NoContent, response.status)

        // Verify company still exists but marked as inactive
        val getResponse = client.get("/companies/$companyId")
        assertEquals(HttpStatusCode.OK, getResponse.status)
        val responseBody = Json.parseToJsonElement(getResponse.bodyAsText()).jsonObject
        assertEquals("\"inactive\"", responseBody["status"].toString())
        assertEquals("\"Company To Delete\"", responseBody["name"].toString()) // Data preserved
    }

    @Test
    fun `DELETE should return 404 when deleting non-existent company`() = testApplication {
        val nonExistentCompanyId = UUID.randomUUID()

        application {
            moduleSharedDb(userId = UUID.randomUUID())
            // No company inserted
        }

        val response = client.delete("/companies/$nonExistentCompanyId")

        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}
