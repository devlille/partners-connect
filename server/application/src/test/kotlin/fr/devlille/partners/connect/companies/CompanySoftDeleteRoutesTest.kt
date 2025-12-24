package fr.devlille.partners.connect.companies

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.internal.moduleSharedDb
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

class CompanySoftDeleteRoutesTest {
    @Test
    fun `DELETE should preserve company relationships after soft delete`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleSharedDb(userId = UUID.randomUUID())
            transaction {
                insertMockedOrganisationEntity(id = orgId)
                insertMockedFutureEvent(id = eventId, orgId = orgId)
                insertMockedCompany(companyId)
                insertMockedPartnership(partnershipId, eventId = eventId, companyId = companyId)
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
            moduleSharedDb(userId = UUID.randomUUID())
            transaction {
                insertMockedCompany(companyId)
            }
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
