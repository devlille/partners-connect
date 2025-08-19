package fr.devlille.partners.connect.events

import fr.devlille.partners.connect.events.domain.Event
import fr.devlille.partners.connect.events.factories.createEvent
import fr.devlille.partners.connect.events.factories.insertMockedEvent
import fr.devlille.partners.connect.events.factories.insertMockedEventWithOrga
import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.users.factories.insertMockedAdminUser
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
import kotlinx.serialization.json.jsonObject
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

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
        assertNotNull(responseBody["slug"], "Response should contain a 'slug' field")
    }

    @Test
    fun `PUT updates an existing event`() = testApplication {
        val eventId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val testSlug = "test-event"
        application {
            moduleMocked()
            insertMockedOrganisationEntity(id = orgId)
            // Create event with specific slug using the updated factory
            insertMockedEvent(id = eventId, orgId = orgId, slug = testSlug)
            insertMockedOrgaPermission(orgId = orgId, user = insertMockedAdminUser())
        }

        val response = client.put("/orgs/$orgId/events/$testSlug") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(Event.serializer(), createEvent()))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val updateBody = Json.decodeFromString<Map<String, String>>(response.bodyAsText())
        assertEquals(testSlug, updateBody["slug"])
    }

    @Test
    fun `PUT returns 401 when user has no access to the event`() = testApplication {
        val organisationId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val testSlug = "test-event-unauthorized"

        application {
            moduleMocked()
            insertMockedEventWithOrga(
                id = eventId,
                slug = testSlug,
                organisation = insertMockedOrganisationEntity(
                    id = organisationId,
                    representativeUser = insertMockedAdminUser(),
                ),
            )
        }

        val updateResponse = client.put("/orgs/$organisationId/events/$testSlug") {
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

    @Test
    fun `GET orgs events returns events for organization with valid user`() = testApplication {
        val orgId = UUID.randomUUID()
        application {
            moduleMocked()
            val admin = insertMockedAdminUser()
            val org = insertMockedOrganisationEntity(id = orgId, representativeUser = admin)
            insertMockedOrgaPermission(orgId = orgId, user = admin)
            insertMockedEventWithOrga(name = "First Event", slug = "first-event", organisation = org)
            insertMockedEventWithOrga(name = "Second Event", slug = "second-event", organisation = org)
        }

        val response = client.get("/orgs/$orgId/events") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody = response.bodyAsText()
        val events = Json.parseToJsonElement(responseBody).jsonArray
        assertEquals(2, events.size)
    }

    @Test
    fun `GET orgs events returns empty list when organization has no events`() = testApplication {
        val orgId = UUID.randomUUID()
        application {
            moduleMocked()
            val admin = insertMockedAdminUser()
            insertMockedOrganisationEntity(id = orgId, representativeUser = admin)
            insertMockedOrgaPermission(orgId = orgId, user = admin)
        }

        val response = client.get("/orgs/$orgId/events") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody = response.bodyAsText()
        val events = Json.parseToJsonElement(responseBody).jsonArray
        assertEquals(0, events.size)
    }

    @Test
    fun `GET orgs events returns 404 when organization does not exist`() = testApplication {
        val nonExistentOrgId = UUID.randomUUID()
        application {
            moduleMocked()
            val admin = insertMockedAdminUser()
            // Don't create any organization or permissions - just test with non-existent org
        }

        val response = client.get("/orgs/$nonExistentOrgId/events") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `GET orgs events returns 401 when user has no permissions`() = testApplication {
        val orgId = UUID.randomUUID()
        application {
            moduleMocked()
            val admin = insertMockedAdminUser()
            val org = insertMockedOrganisationEntity(id = orgId, representativeUser = admin)
            insertMockedEventWithOrga(organisation = org)
            // No organization permission granted to user
        }

        val response = client.get("/orgs/$orgId/events") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `GET orgs events returns 401 when no authorization header provided`() = testApplication {
        val orgId = UUID.randomUUID()
        application {
            moduleMocked()
            val admin = insertMockedAdminUser()
            val org = insertMockedOrganisationEntity(id = orgId, representativeUser = admin)
            insertMockedEventWithOrga(organisation = org)
            insertMockedOrgaPermission(orgId = orgId, user = admin)
        }

        val response = client.get("/orgs/$orgId/events")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `GET events by slug returns event with organization for valid slug`() = testApplication {
        val eventId = UUID.randomUUID()
        val eventSlug = "test-event"
        application {
            moduleMocked()
            val admin = insertMockedAdminUser()
            val org = insertMockedOrganisationEntity(representativeUser = admin)
            insertMockedEventWithOrga(id = eventId, slug = eventSlug, organisation = org)
        }

        val response = client.get("/events/$eventSlug")

        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody = response.bodyAsText()
        val eventWithOrg = Json.parseToJsonElement(responseBody).jsonObject

        // Verify response structure
        assert(eventWithOrg.containsKey("event"))
        assert(eventWithOrg.containsKey("organisation"))

        // Verify event structure
        val eventObject = eventWithOrg["event"]?.jsonObject
        assertNotNull(eventObject)
        assert(eventObject!!.containsKey("name"))
        assert(eventObject.containsKey("start_time"))
        assert(eventObject.containsKey("end_time"))
        assert(eventObject.containsKey("submission_start_time"))
        assert(eventObject.containsKey("submission_end_time"))
        assert(eventObject.containsKey("address"))
        assert(eventObject.containsKey("contact"))

        // Verify organisation structure
        val organisationObject = eventWithOrg["organisation"]?.jsonObject
        assertNotNull(organisationObject)
        assert(organisationObject!!.containsKey("name"))
        assert(organisationObject.containsKey("head_office"))
        assert(organisationObject.containsKey("representative_user_email"))
        assert(organisationObject.containsKey("representative_role"))
        assert(organisationObject.containsKey("created_at"))
        assert(organisationObject.containsKey("published_at"))
    }

    @Test
    fun `GET events by slug returns 404 for non-existent event`() = testApplication {
        val nonExistentEventSlug = "non-existent-event"
        application {
            moduleMocked()
        }

        val response = client.get("/events/$nonExistentEventSlug")

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `GET events by slug accepts various slug formats`() = testApplication {
        application {
            moduleMocked()
        }

        val response = client.get("/events/valid-event-slug")

        // This should return 404 (not found) rather than 400 (bad request)
        // since slug format is valid, even if event doesn't exist
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `GET events by slug is public and returns correct response structure`() = testApplication {
        val eventId = UUID.randomUUID()
        val eventSlug = "public-test-event"
        application {
            moduleMocked()
            val admin = insertMockedAdminUser()
            val org = insertMockedOrganisationEntity(representativeUser = admin)
            insertMockedEventWithOrga(id = eventId, slug = eventSlug, organisation = org)
        }

        // No authentication header - this is a public endpoint
        val response = client.get("/events/$eventSlug")

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
