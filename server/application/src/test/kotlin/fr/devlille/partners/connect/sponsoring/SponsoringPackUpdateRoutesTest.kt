package fr.devlille.partners.connect.sponsoring

import fr.devlille.partners.connect.events.factories.insertMockedEvent
import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.sponsoring.domain.SponsoringPack
import fr.devlille.partners.connect.sponsoring.factories.createSponsoringPack
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringPack
import fr.devlille.partners.connect.sponsoring.infrastructure.db.SponsoringPackEntity
import fr.devlille.partners.connect.users.factories.insertMockedEventWithAdminUser
import io.ktor.client.request.get
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

class SponsoringPackUpdateRoutesTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `PUT updates pack successfully`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val eventSlug = "test-put-updates-pack-suc-53"
        val packId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)

            insertMockedSponsoringPack(
                id = packId,
                event = eventId,
                name = "Original Pack",
                basePrice = 1000,
                maxQuantity = 5,
                nbTickets = 2,
                withBooth = false,
            )
        }

        val updateRequest = createSponsoringPack(
            name = "Updated Pack",
            price = 2500,
            maxQuantity = 15,
            nbTickets = 5,
            withBooth = true,
        )

        val response = client.put("/orgs/$orgId/events/$eventSlug/packs/$packId") {
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
        assertEquals(5, updatedPack.nbTickets)
        assertEquals(true, updatedPack.withBooth)
    }

    @Test
    fun `PUT verifies updated pack via GET endpoint`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val eventSlug = "test-put-verifies-updated-565"
        val packId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)

            insertMockedSponsoringPack(
                id = packId,
                event = eventId,
                name = "Original Pack",
                basePrice = 1000,
                maxQuantity = 5,
            )
        }

        val updateRequest = createSponsoringPack(
            name = "Updated Pack Name",
            price = 3000,
            maxQuantity = 20,
        )

        // Update the pack
        client.put("/orgs/$orgId/events/$eventSlug/packs/$packId") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(Json.encodeToString(updateRequest))
        }

        // Verify via GET endpoint
        val getResponse = client.get("/orgs/$orgId/events/$eventSlug/packs") {
            header(HttpHeaders.AcceptLanguage, "en")
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, getResponse.status)
        val packs = json.decodeFromString<List<SponsoringPack>>(getResponse.bodyAsText())
        assertEquals(1, packs.size)
        assertEquals("Updated Pack Name", packs[0].name)
        assertEquals(3000, packs[0].basePrice)
        assertEquals(20, packs[0].maxQuantity)
    }

    @Test
    fun `PUT fails with not found when pack does not exist`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val eventSlug = "test-put-fails-with-not-f-24"
        val nonExistentPackId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)
        }

        val response = client.put("/orgs/$orgId/events/$eventSlug/packs/$nonExistentPackId") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(Json.encodeToString(createSponsoringPack()))
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `PUT fails with bad request when payload is invalid`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val eventSlug = "test-put-fails-with-bad-r-689"
        val packId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)

            insertMockedSponsoringPack(id = packId, event = eventId)
        }

        val response = client.put("/orgs/$orgId/events/$eventSlug/packs/$packId") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody("""{"invalid": "payload"}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `PUT fails with not found when user lacks org permission`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val eventSlug = "test-put-fails-with-not-f-135"
        val packId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            // Create event without admin user permissions (no permission setup)
            insertMockedEvent(eventId, slug = eventSlug, orgId = orgId)
            insertMockedSponsoringPack(id = packId, event = eventId)
        }

        val response = client.put("/orgs/$orgId/events/$eventSlug/packs/$packId") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(Json.encodeToString(createSponsoringPack()))
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}
