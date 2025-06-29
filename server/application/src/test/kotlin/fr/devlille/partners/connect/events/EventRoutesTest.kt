package fr.devlille.partners.connect.events

import fr.devlille.partners.connect.events.infrastructure.api.CreateOrUpdateEventResponse
import fr.devlille.partners.connect.module
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
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
    fun testCreateEvent() = testApplication {
        application { module() }

        val response = client.post("/events") {
            contentType(ContentType.Application.Json)
            setBody(testEventJson)
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val responseText = response.bodyAsText()
        val responseBody = Json.decodeFromString<CreateOrUpdateEventResponse>(responseText)
        assertNotNull(responseBody.id)
    }

    @Test
    fun testUpdateEvent() = testApplication {
        application { module() }

        // First, create the event
        val createResponse = client.post("/events") {
            contentType(ContentType.Application.Json)
            setBody(testEventJson)
        }
        val createResponseText = createResponse.bodyAsText()
        val createResponseBody = Json.decodeFromString<CreateOrUpdateEventResponse>(createResponseText)

        // Then, update the event
        val updateResponse = client.put("/events/${createResponseBody.id}") {
            contentType(ContentType.Application.Json)
            setBody(testEventJson)
        }

        assertEquals(HttpStatusCode.OK, updateResponse.status)
        val updateResponseText = updateResponse.bodyAsText()
        val updateResponseBody = Json.decodeFromString<CreateOrUpdateEventResponse>(updateResponseText)
        assertEquals(createResponseBody.id, updateResponseBody.id)
    }

    @Test
    fun testGetAllEvents() = testApplication {
        application { module() }

        // Ensure one event exists
        client.post("/events") {
            contentType(ContentType.Application.Json)
            setBody(testEventJson)
        }

        val response = client.get("/events")
        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody = response.bodyAsText()
        assert(Json.parseToJsonElement(responseBody).jsonArray.isNotEmpty())
    }
}
