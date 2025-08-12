package fr.devlille.partners.connect.events

import fr.devlille.partners.connect.internal.insertMockedEvent
import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.legalentity.factories.insertLegalEntity
import fr.devlille.partners.connect.users.factories.insertMockedAdminUser
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
    private fun testEvent(legalEntityId: String) = """
        {
            "name": "DevLille 2025",
            "start_time": "2025-06-12T09:00:00",
            "end_time": "2025-06-13T18:00:00",
            "submission_start_time": "2025-01-01T00:00:00",
            "submission_end_time": "2025-03-01T23:59:59",
            "address": "Lille Grand Palais, Lille, France",
            "contact": {
                "phone": "+33 6 12 34 56 78",
                "email": "contact@devlille.fr"
            },
            "legal_entity_id": "$legalEntityId"
        }
    """

    @Test
    fun `POST creates an event and grants access to creator`() = testApplication {
        val legalEntityId = UUID.randomUUID()
        application {
            moduleMocked()
            insertLegalEntity(id = legalEntityId, representativeUser = insertMockedAdminUser())
        }

        val response = client.post("/events") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(testEvent(legalEntityId.toString()))
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val responseText = response.bodyAsText()
        val responseBody = Json.decodeFromString<Map<String, String>>(responseText)
        assertNotNull(responseBody["id"], "Response should contain an 'id' field")
    }

    @Test
    fun `PUT updates an existing event`() = testApplication {
        val legalEntityId = UUID.randomUUID()
        application {
            moduleMocked()
            insertLegalEntity(id = legalEntityId, representativeUser = insertMockedAdminUser())
        }

        val createResponse = client.post("/events") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(testEvent(legalEntityId.toString()))
        }

        val responseBody = Json.decodeFromString<Map<String, String>>(createResponse.bodyAsText())
        val createdId = responseBody["id"]
        assertNotNull(createdId)

        val updateResponse = client.put("/events/$createdId") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(testEvent(legalEntityId.toString()))
        }

        assertEquals(HttpStatusCode.OK, updateResponse.status)
        val updateBody = Json.decodeFromString<Map<String, String>>(updateResponse.bodyAsText())
        assertEquals(createdId, updateBody["id"])
    }

    @Test
    fun `PUT returns 401 when user has no access to the event`() = testApplication {
        val legalEntityId = UUID.randomUUID()
        val eventId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedEvent(
                eventId,
                legalEntity = insertLegalEntity(id = legalEntityId, representativeUser = insertMockedAdminUser()),
            )
        }

        val updateResponse = client.put("/events/$eventId") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(testEvent(legalEntityId.toString()))
        }

        assertEquals(HttpStatusCode.Unauthorized, updateResponse.status)
    }

    @Test
    fun `GET returns all events`() = testApplication {
        val legalEntityId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        application {
            moduleMocked()
            insertLegalEntity(id = legalEntityId, representativeUser = insertMockedAdminUser())
        }

        client.post("/events") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(testEvent(legalEntityId.toString()))
        }

        val response = client.get("/events")
        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody = response.bodyAsText()
        assert(Json.parseToJsonElement(responseBody).jsonArray.isNotEmpty())
    }
}
