package fr.devlille.partners.connect.users

import fr.devlille.partners.connect.events.factories.insertMockedEvent
import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import fr.devlille.partners.connect.users.factories.insertMockedUser
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ListUserEventsRouteTest {
    @Test
    fun `return 401 if no Authorization header`() = testApplication {
        application {
            moduleMocked()
        }

        val response = client.get("/users/me/events")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `return 401 if token is expired or invalid`() = testApplication {
        application {
            moduleMocked()
        }

        val response = client.get("/users/me/events") {
            header("Authorization", "Bearer invalid")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `return 404 if authenticated user is not in DB`() = testApplication {
        application {
            moduleMocked()
        }

        val response = client.get("/users/me/events") {
            header("Authorization", "Bearer valid")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `return 403 if authenticated user has no organizer permissions`() = testApplication {
        val userId = UUID.randomUUID()
        val email = "john.doe@contact.com" // Must match the mock auth email

        application {
            moduleMocked()
            insertMockedUser(userId, email = email)
        }

        val response = client.get("/users/me/events") {
            header("Authorization", "Bearer valid")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `return empty array if organizer has no events`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val email = "john.doe@contact.com" // Must match the mock auth email

        application {
            moduleMocked()
            val user = insertMockedUser(userId, email = email)
            insertMockedOrganisationEntity(orgId)
            insertMockedOrgaPermission(orgId = orgId, user = user, canEdit = true)
        }

        val response = client.get("/users/me/events") {
            header("Authorization", "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("[]", response.bodyAsText())
    }

    @Test
    fun `return events array if organizer has events`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val email = "john.doe@contact.com" // Must match the mock auth email

        application {
            moduleMocked()
            val user = insertMockedUser(userId, email = email)
            insertMockedOrganisationEntity(orgId)
            insertMockedEvent(eventId, orgId = orgId, name = "Test Event 2025")
            insertMockedOrgaPermission(orgId = orgId, user = user, canEdit = true)
        }

        val response = client.get("/users/me/events") {
            header("Authorization", "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)

        val json = Json.parseToJsonElement(response.bodyAsText())
        val eventsArray = json.jsonArray

        assertEquals(1, eventsArray.size)

        val event = eventsArray[0].jsonObject
        assertEquals(eventId.toString(), event["id"]?.jsonPrimitive?.content)
        assertEquals("Test Event 2025", event["name"]?.jsonPrimitive?.content)
        assertTrue(event.containsKey("start_time"))
        assertTrue(event.containsKey("end_time"))
        assertTrue(event.containsKey("submission_start_time"))
        assertTrue(event.containsKey("submission_end_time"))
    }

    @Test
    fun `return multiple events from multiple organizations`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId1 = UUID.randomUUID()
        val orgId2 = UUID.randomUUID()
        val eventId1 = UUID.randomUUID()
        val eventId2 = UUID.randomUUID()
        val email = "john.doe@contact.com" // Must match the mock auth email

        application {
            moduleMocked()
            val user = insertMockedUser(userId, email = email)

            // First organization with one event
            insertMockedOrganisationEntity(orgId1)
            insertMockedEvent(eventId1, orgId = orgId1, name = "Event 1")
            insertMockedOrgaPermission(orgId = orgId1, user = user, canEdit = true)

            // Second organization with one event
            insertMockedOrganisationEntity(orgId2)
            insertMockedEvent(eventId2, orgId = orgId2, name = "Event 2")
            insertMockedOrgaPermission(orgId = orgId2, user = user, canEdit = true)
        }

        val response = client.get("/users/me/events") {
            header("Authorization", "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)

        val json = Json.parseToJsonElement(response.bodyAsText())
        val eventsArray = json.jsonArray

        assertEquals(2, eventsArray.size)

        val eventIds = eventsArray.map { it.jsonObject["id"]?.jsonPrimitive?.content }
        assertTrue(eventIds.contains(eventId1.toString()))
        assertTrue(eventIds.contains(eventId2.toString()))
    }
}
