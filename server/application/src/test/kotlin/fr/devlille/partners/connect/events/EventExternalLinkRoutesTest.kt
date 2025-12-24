package fr.devlille.partners.connect.events

import fr.devlille.partners.connect.events.domain.CreateEventExternalLinkRequest
import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import fr.devlille.partners.connect.users.factories.insertMockedUser
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
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
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EventExternalLinkRoutesTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `DELETE external-link removes external link and returns 204`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()

        application {
            moduleSharedDb(userId = userId)
            transaction {
                insertMockedOrganisationEntity(orgId)
                insertMockedUser(userId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
            }
        }

        // First create an external link
        val createRequest = CreateEventExternalLinkRequest(
            name = "Call for Papers",
            url = "https://sessionize.com/devlille2025",
        )

        val createResponse = client.post("/orgs/$orgId/events/$eventId/external-link") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(createRequest))
        }

        assertEquals(HttpStatusCode.Created, createResponse.status)
        val createdLinkId = json.decodeFromString<Map<String, String>>(createResponse.bodyAsText())["id"]

        // Then delete it
        val deleteResponse = client.delete("/orgs/$orgId/events/$eventId/external-link/$createdLinkId") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.NoContent, deleteResponse.status)
    }

    @Test
    fun `GET events by slug includes external links in response`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()

        application {
            moduleSharedDb(userId = userId)
            transaction {
                insertMockedOrganisationEntity(orgId)
                insertMockedUser(userId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
            }
        }

        // First, create an external link
        val createRequest = CreateEventExternalLinkRequest(
            name = "Call for Papers",
            url = "https://sessionize.com/devlille2025",
        )

        val createResponse = client.post("/orgs/$orgId/events/$eventId/external-link") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(Json.encodeToString(createRequest))
        }

        assertEquals(HttpStatusCode.Created, createResponse.status)

        // Now get the event and verify the external link is included
        val getResponse = client.get("/events/$eventId")

        assertEquals(HttpStatusCode.OK, getResponse.status)
        val responseBody = getResponse.bodyAsText()
        val responseJson = Json.parseToJsonElement(responseBody).jsonObject

        val event = responseJson["event"]!!.jsonObject
        assertTrue(event.containsKey("external_links"))

        val externalLinks = event["external_links"]!!.jsonArray
        assertEquals(1, externalLinks.size)

        val externalLink = externalLinks[0].jsonObject
        assertTrue(externalLink.containsKey("id"))
        assertEquals("Call for Papers", externalLink["name"]!!.toString().removeSurrounding("\""))
        assertEquals("https://sessionize.com/devlille2025", externalLink["url"]!!.toString().removeSurrounding("\""))
    }
}
