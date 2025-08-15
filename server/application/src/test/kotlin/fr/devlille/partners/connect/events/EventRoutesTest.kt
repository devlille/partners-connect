package fr.devlille.partners.connect.events

import fr.devlille.partners.connect.events.domain.Event
import fr.devlille.partners.connect.events.factories.createEvent
import fr.devlille.partners.connect.events.factories.insertMockedEventWithOrga
import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.users.factories.insertMockedAdminUser
import fr.devlille.partners.connect.users.factories.insertMockedEventWithAdminUser
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
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
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class EventRoutesTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `POST creates an event and grants access to creator`() = testApplication {
        val orgId = UUID.randomUUID()
        application {
            moduleMocked()
            val admin = insertMockedAdminUser()
            insertMockedOrganisationEntity(id = orgId, representativeUser = admin)
            insertMockedOrgaPermission(orgId = orgId, user = admin)
        }

        val response = client.post("/orgs/$orgId/events") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(Event.serializer(), createEvent()))
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val responseText = response.bodyAsText()
        val responseBody = Json.decodeFromString<Map<String, String>>(responseText)
        assertNotNull(responseBody["id"], "Response should contain an 'id' field")
    }

    @Test
    fun `PUT updates an existing event`() = testApplication {
        val eventId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        application {
            moduleMocked()
            insertMockedOrganisationEntity(id = orgId)
            insertMockedEventWithAdminUser(eventId = eventId, orgId = orgId)
        }

        val response = client.put("/orgs/$orgId/events/$eventId") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(Event.serializer(), createEvent()))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val updateBody = Json.decodeFromString<Map<String, String>>(response.bodyAsText())
        assertEquals(eventId.toString(), updateBody["id"])
    }

    @Test
    fun `PUT returns 401 when user has no access to the event`() = testApplication {
        val organisationId = UUID.randomUUID()
        val eventId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedEventWithOrga(
                id = eventId,
                organisation = insertMockedOrganisationEntity(
                    id = organisationId,
                    representativeUser = insertMockedAdminUser(),
                ),
            )
        }

        val updateResponse = client.put("/orgs/$organisationId/events/$eventId") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(Event.serializer(), createEvent()))
        }

        assertEquals(HttpStatusCode.Unauthorized, updateResponse.status)
    }

    @Test
    fun `GET returns all events`() = testApplication {
        val orgId = UUID.randomUUID()
        application {
            moduleMocked()
            val org = insertMockedOrganisationEntity(id = orgId, representativeUser = insertMockedAdminUser())
            insertMockedEventWithOrga(organisation = org)
        }

        val response = client.get("/events")
        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody = response.bodyAsText()
        assert(Json.parseToJsonElement(responseBody).jsonArray.isNotEmpty())
    }
}
