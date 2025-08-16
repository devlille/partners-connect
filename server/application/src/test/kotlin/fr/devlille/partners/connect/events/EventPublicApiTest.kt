package fr.devlille.partners.connect.events

import fr.devlille.partners.connect.events.factories.insertMockedEventWithOrga
import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.users.factories.insertMockedAdminUser
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EventPublicApiTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `GET events by ID is public and returns correct response structure`() = testApplication {
        val eventId = UUID.randomUUID()
        application {
            moduleMocked()
            val admin = insertMockedAdminUser()
            val org = insertMockedOrganisationEntity(representativeUser = admin)
            insertMockedEventWithOrga(id = eventId, organisation = org)
        }

        // No authentication header - this is a public endpoint
        val response = client.get("/events/$eventId")

        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody = response.bodyAsText()
        val responseJson = Json.parseToJsonElement(responseBody).jsonObject

        // Verify top-level structure
        assertTrue(responseJson.containsKey("event"))
        assertTrue(responseJson.containsKey("organisation"))

        // Verify event structure has required fields
        val event = responseJson["event"]!!.jsonObject
        assertTrue(event.containsKey("name"))
        assertTrue(event.containsKey("start_time"))
        assertTrue(event.containsKey("end_time"))
        assertTrue(event.containsKey("submission_start_time"))
        assertTrue(event.containsKey("submission_end_time"))
        assertTrue(event.containsKey("address"))
        assertTrue(event.containsKey("contact"))

        // Verify organization structure has required fields
        val organisation = responseJson["organisation"]!!.jsonObject
        assertTrue(organisation.containsKey("name"))
        assertTrue(organisation.containsKey("head_office"))
        assertTrue(organisation.containsKey("representative_user_email"))
        assertTrue(organisation.containsKey("representative_role"))
    }
}