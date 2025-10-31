package fr.devlille.partners.connect.companies

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.events.factories.insertMockedEvent
import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.partnership.factories.insertMockedPartnership
import fr.devlille.partners.connect.partnership.infrastructure.db.PartnershipEntity
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
import kotlin.test.assertNotNull

class CompanySoftDeleteIntegrationTest {
    @Test
    fun `DELETE should soft delete company and return 204`() = testApplication {
        val companyId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedCompany(companyId, name = "Company To Delete")
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
            moduleMocked()
            // No company inserted
        }

        val response = client.delete("/companies/$nonExistentCompanyId")

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `DELETE should preserve company relationships after soft delete`() = testApplication {
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleMocked()
            transaction {
                val org = insertMockedOrganisationEntity()
                val event = insertMockedEvent(orgId = org.id.value)
                insertMockedCompany(companyId)
                insertMockedPartnership(partnershipId, eventId = event.id.value, companyId = companyId)
            }
        }

        // Soft delete company
        val deleteResponse = client.delete("/companies/$companyId")
        assertEquals(HttpStatusCode.NoContent, deleteResponse.status)

        // Verify company still accessible (soft deleted)
        val companyResponse = client.get("/companies/$companyId")
        assertEquals(HttpStatusCode.OK, companyResponse.status)
        val responseBody = Json.parseToJsonElement(companyResponse.bodyAsText()).jsonObject
        assertEquals("\"inactive\"", responseBody["status"].toString())

        val partnership = transaction { PartnershipEntity.findById(partnershipId) }
        assertNotNull(partnership)
        assertEquals(companyId, transaction { partnership.company }.id.value)
    }

    @Test
    fun `DELETE should be idempotent - deleting already deleted company returns 204`() = testApplication {
        val companyId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedCompany(companyId)
        }

        // First delete
        val firstDeleteResponse = client.delete("/companies/$companyId")
        assertEquals(HttpStatusCode.NoContent, firstDeleteResponse.status)

        // Second delete should also return 204 (idempotent)
        val secondDeleteResponse = client.delete("/companies/$companyId")
        assertEquals(HttpStatusCode.NoContent, secondDeleteResponse.status)

        // Company should still be inactive
        val getResponse = client.get("/companies/$companyId")
        assertEquals(HttpStatusCode.OK, getResponse.status)
        val responseBody = Json.parseToJsonElement(getResponse.bodyAsText()).jsonObject
        assertEquals("\"inactive\"", responseBody["status"].toString())
    }
}
