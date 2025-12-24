package fr.devlille.partners.connect.sponsoring.infrastructure.api

import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.sponsoring.factories.createSponsoringPack
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringPack
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringPackEntity
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import fr.devlille.partners.connect.users.factories.insertMockedUser
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SponsoringPackUpdateRoutePutTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `PUT updates pack successfully`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedSponsoringPack(
                    id = packId,
                    eventId = eventId,
                    basePrice = 1000,
                    maxQuantity = 5,
                )
            }
        }

        val updateRequest = createSponsoringPack(
            name = "Updated Pack",
            price = 2500,
            maxQuantity = 15,
        )

        val response = client.put("/orgs/$orgId/events/$eventId/packs/$packId") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(Json.encodeToString(updateRequest))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody = json.decodeFromString<Map<String, String>>(response.bodyAsText())
        assertEquals(packId.toString(), responseBody["id"])

        // Verify the pack was actually updated in the database
        val updatedPack = transaction {
            SponsoringPackEntity.findById(packId)
        }

        assertNotNull(updatedPack)
        assertEquals("Updated Pack", updatedPack.name)
        assertEquals(2500, updatedPack.basePrice)
        assertEquals(15, updatedPack.maxQuantity)
    }

    @Test
    fun `PUT fails with bad request when payload is invalid`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedSponsoringPack(id = packId, eventId = eventId)
            }
        }

        val response = client.put("/orgs/$orgId/events/$eventId/packs/$packId") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody("""{"invalid": "payload"}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `PUT fails with not found when user lacks org permission`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedSponsoringPack(id = packId, eventId = eventId)
            }
        }

        val response = client.put("/orgs/$orgId/events/$eventId/packs/$packId") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(Json.encodeToString(createSponsoringPack()))
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `PUT fails with not found when pack does not exist`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val nonExistentPackId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
            }
        }

        val response = client.put("/orgs/$orgId/events/$eventId/packs/$nonExistentPackId") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(Json.encodeToString(createSponsoringPack()))
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}
