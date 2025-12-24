package fr.devlille.partners.connect.events.infrastructure.api

import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import fr.devlille.partners.connect.users.factories.insertMockedUser
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class EventBySlugRouteGetTest {
    @Test
    fun `GET events by slug returns event with organization for valid slug`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedOrganisationEntity(orgId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedUser(userId)
                insertMockedOrgaPermission(orgId = orgId, userId = userId)
            }
        }

        val response = client.get("/events/$eventId")

        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody = response.bodyAsText()
        val eventWithOrg = Json.parseToJsonElement(responseBody).jsonObject

        // Verify response structure
        assert(eventWithOrg.containsKey("event"))
        assert(eventWithOrg.containsKey("organisation"))

        // Verify event structure
        val eventObject = eventWithOrg["event"]?.jsonObject
        assertNotNull(eventObject)
        assertTrue(eventObject.containsKey("slug"))
        assertTrue(eventObject.containsKey("name"))
        assertTrue(eventObject.containsKey("start_time"))
        assertTrue(eventObject.containsKey("end_time"))
        assertTrue(eventObject.containsKey("submission_start_time"))
        assertTrue(eventObject.containsKey("submission_end_time"))
        assertTrue(eventObject.containsKey("address"))
        assertTrue(eventObject.containsKey("contact"))

        // Verify external_links is an array (should be empty for this test)
        assertTrue(eventObject.containsKey("external_links"))
        val externalLinks = eventObject["external_links"]!!.jsonArray
        assertEquals(0, externalLinks.size)

        // Verify providers field exists and is empty
        assertTrue(eventObject.containsKey("providers"))
        val providers = eventObject["providers"]!!.jsonArray
        assertEquals(0, providers.size)

        // Verify organisation structure
        val organisationObject = eventWithOrg["organisation"]?.jsonObject
        assertNotNull(organisationObject)
        assert(organisationObject.containsKey("name"))
        assert(organisationObject.containsKey("slug"))
        assert(organisationObject.containsKey("head_office"))
        assert(organisationObject.containsKey("owner"))
    }

    @Test
    fun `GET events by slug returns 404 for non-existent event`() = testApplication {
        val userId = UUID.randomUUID()
        val nonExistentEventSlug = "non-existent-event"

        application {
            moduleSharedDb(userId)
        }

        val response = client.get("/events/$nonExistentEventSlug")

        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}
