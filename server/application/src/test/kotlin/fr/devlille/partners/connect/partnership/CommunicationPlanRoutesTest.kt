package fr.devlille.partners.connect.partnership

import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import fr.devlille.partners.connect.users.factories.insertMockedUser
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
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

class CommunicationPlanRoutesTest {
    @Test
    @Suppress("LongMethod")
    fun `full CRUD lifecycle for standalone communication plan entry`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
            }
        }

        // POST — create a standalone entry
        val postResponse = client.post("/orgs/$orgId/events/$eventId/communication-plan") {
            header(HttpHeaders.Authorization, "Bearer valid")
            contentType(ContentType.Application.Json)
            setBody(
                """{
                    "title": "Blog post announcement",
                    "scheduled_date": "2026-06-15T10:00:00",
                    "description": "Our annual blog post",
                    "support_url": "https://example.com/blog.png"
                }""",
            )
        }

        assertEquals(HttpStatusCode.Created, postResponse.status)

        val createdEntry = Json.parseToJsonElement(postResponse.bodyAsText()).jsonObject
        val entryId = createdEntry["id"]!!.jsonPrimitive.content
        assertEquals("Blog post announcement", createdEntry["title"]!!.jsonPrimitive.content)
        assertTrue(!createdEntry.containsKey("partnership_id") || createdEntry["partnership_id"] == null)

        // GET communication plan — entry appears in planned group
        val getAfterCreate = client.get("/orgs/$orgId/events/$eventId/communication") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, getAfterCreate.status)
        val getBody = Json.parseToJsonElement(getAfterCreate.bodyAsText()).jsonObject
        val planned = getBody["planned"]!!.jsonArray
        assertEquals(1, planned.size)
        assertEquals("Blog post announcement", planned[0].jsonObject["title"]!!.jsonPrimitive.content)
        assertTrue(planned[0].jsonObject["partnership_id"] == null)

        // PUT — update the entry title
        val putResponse = client.put("/orgs/$orgId/events/$eventId/communication-plan/$entryId") {
            header(HttpHeaders.Authorization, "Bearer valid")
            contentType(ContentType.Application.Json)
            setBody(
                """{
                    "title": "Updated announcement",
                    "scheduled_date": "2026-06-15T10:00:00"
                }""",
            )
        }

        assertEquals(HttpStatusCode.OK, putResponse.status)
        val updatedEntry = Json.parseToJsonElement(putResponse.bodyAsText()).jsonObject
        assertEquals("Updated announcement", updatedEntry["title"]!!.jsonPrimitive.content)
        assertEquals(entryId, updatedEntry["id"]!!.jsonPrimitive.content)

        // GET — reflects the update
        val getAfterUpdate = client.get("/orgs/$orgId/events/$eventId/communication") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, getAfterUpdate.status)
        val getBodyAfterUpdate = Json.parseToJsonElement(getAfterUpdate.bodyAsText()).jsonObject
        val plannedAfterUpdate = getBodyAfterUpdate["planned"]!!.jsonArray
        assertEquals(1, plannedAfterUpdate.size)
        assertEquals("Updated announcement", plannedAfterUpdate[0].jsonObject["title"]!!.jsonPrimitive.content)

        // DELETE — remove the entry
        val deleteResponse = client.delete("/orgs/$orgId/events/$eventId/communication-plan/$entryId") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.NoContent, deleteResponse.status)

        // GET — entry is gone
        val getAfterDelete = client.get("/orgs/$orgId/events/$eventId/communication") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, getAfterDelete.status)
        val getBodyAfterDelete = Json.parseToJsonElement(getAfterDelete.bodyAsText()).jsonObject
        assertTrue(getBodyAfterDelete["planned"]!!.jsonArray.isEmpty())
        assertTrue(getBodyAfterDelete["done"]!!.jsonArray.isEmpty())
        assertTrue(getBodyAfterDelete["unplanned"]!!.jsonArray.isEmpty())
    }

    @Test
    fun `standalone entry without scheduled date appears in unplanned group`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
            }
        }

        val postResponse = client.post("/orgs/$orgId/events/$eventId/communication-plan") {
            header(HttpHeaders.Authorization, "Bearer valid")
            contentType(ContentType.Application.Json)
            setBody("""{"title": "Unscheduled post", "scheduled_date": null}""")
        }

        assertEquals(HttpStatusCode.Created, postResponse.status)

        val getResponse = client.get("/orgs/$orgId/events/$eventId/communication") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, getResponse.status)
        val body = Json.parseToJsonElement(getResponse.bodyAsText()).jsonObject
        val unplanned = body["unplanned"]!!.jsonArray
        assertEquals(1, unplanned.size)
        assertEquals("Unscheduled post", unplanned[0].jsonObject["title"]!!.jsonPrimitive.content)
        assertTrue(unplanned[0].jsonObject["partnership_id"] == null)
        assertTrue(body["planned"]!!.jsonArray.isEmpty())
        assertTrue(body["done"]!!.jsonArray.isEmpty())
    }
}
