package fr.devlille.partners.connect.events

import fr.devlille.partners.connect.internal.insertMockedAdminUser
import fr.devlille.partners.connect.internal.insertMockedEvent
import fr.devlille.partners.connect.internal.insertMockedUser
import fr.devlille.partners.connect.internal.moduleMocked
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
    private val testEventJson = """
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
            "legal": {
                "name": "DevLille Org",
                "siret": "12345678900019",
                "siren": "123456789",
                "tva": "FR123456789",
                "d_and_b": "123456789",
                "nace": "62.01Z",
                "naf": "62.01Z",
                "duns": "987654321"
            },
            "banking": {
                "iban": "FR7630006000011234567890189",
                "bic": "AGRIFRPPXXX",
                "rib_url": "https://example.com/rib.pdf"
            }
        }
    """

    @Test
    fun `POST creates an event and grants access to creator`() = testApplication {
        application {
            moduleMocked()
            insertMockedAdminUser(UUID.randomUUID())
        }

        val response = client.post("/events") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(testEventJson)
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val responseText = response.bodyAsText()
        val responseBody = Json.decodeFromString<Map<String, String>>(responseText)
        assertNotNull(responseBody["id"], "Response should contain an 'id' field")
    }

    @Test
    fun `PUT updates an existing event`() = testApplication {
        application {
            moduleMocked()
            insertMockedAdminUser(UUID.randomUUID())
        }

        val createResponse = client.post("/events") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(testEventJson)
        }

        val responseBody = Json.decodeFromString<Map<String, String>>(createResponse.bodyAsText())
        val createdId = responseBody["id"]
        assertNotNull(createdId)

        val updateResponse = client.put("/events/$createdId") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(testEventJson)
        }

        assertEquals(HttpStatusCode.OK, updateResponse.status)
        val updateBody = Json.decodeFromString<Map<String, String>>(updateResponse.bodyAsText())
        assertEquals(createdId, updateBody["id"])
    }

    @Test
    fun `PUT returns 401 when user has no access to the event`() = testApplication {
        val eventId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedEvent(eventId)
            insertMockedUser()
        }

        val updateResponse = client.put("/events/$eventId") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(testEventJson)
        }

        assertEquals(HttpStatusCode.Unauthorized, updateResponse.status)
    }

    @Test
    fun `GET returns all events`() = testApplication {
        val eventId = UUID.randomUUID()
        application {
            moduleMocked()
            insertMockedAdminUser(eventId)
        }

        client.post("/events") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(testEventJson)
        }

        val response = client.get("/events")
        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody = response.bodyAsText()
        assert(Json.parseToJsonElement(responseBody).jsonArray.isNotEmpty())
    }
}
