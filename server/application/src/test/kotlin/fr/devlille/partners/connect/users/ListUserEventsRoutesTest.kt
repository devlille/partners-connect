package fr.devlille.partners.connect.users

import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.internal.moduleSharedDb
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
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ListUserEventsRoutesTest {
    @Test
    fun `return events array if organizer has events`() = testApplication {
        val orgId = UUID.randomUUID()
        val userId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId)
                insertMockedFutureEvent(orgId = orgId)
            }
        }

        val response = client.get("/users/me/events") {
            header("Authorization", "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)

        val json = Json.parseToJsonElement(response.bodyAsText())
        val eventsArray = json.jsonArray

        assertTrue(eventsArray.isNotEmpty())
    }

    @Test
    fun `return multiple events from multiple organizations`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId1 = UUID.randomUUID()
        val orgId2 = UUID.randomUUID()
        val eventId1 = UUID.randomUUID()
        val eventId2 = UUID.randomUUID()
        val eventSlug1 = "event-1"
        val eventSlug2 = "event-2"
        val email = "john.doe.$userId@contact.com"

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId, email = email)

                // First organization with one event
                insertMockedOrganisationEntity(orgId1)
                insertMockedFutureEvent(eventId1, orgId = orgId1, name = "Event 1", slug = eventSlug1)
                insertMockedOrgaPermission(orgId = orgId1, userId = userId, canEdit = true)

                // Second organization with one event
                insertMockedOrganisationEntity(orgId2)
                insertMockedFutureEvent(eventId2, orgId = orgId2, name = "Event 2", slug = eventSlug2)
                insertMockedOrgaPermission(orgId = orgId2, userId = userId, canEdit = true)
            }
        }

        val response = client.get("/users/me/events") {
            header("Authorization", "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)

        val json = Json.parseToJsonElement(response.bodyAsText())
        val eventsArray = json.jsonArray

        assertEquals(2, eventsArray.size)

        val eventSlugs = eventsArray.map { it.jsonObject["slug"]?.jsonPrimitive?.content }
        assertTrue(eventSlugs.contains(eventSlug1))
        assertTrue(eventSlugs.contains(eventSlug2))
    }
}
