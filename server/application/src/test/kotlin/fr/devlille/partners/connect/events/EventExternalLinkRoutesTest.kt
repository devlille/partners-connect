package fr.devlille.partners.connect.events

import fr.devlille.partners.connect.events.domain.CreateEventExternalLinkRequest
import fr.devlille.partners.connect.events.factories.insertMockedEventWithOrga
import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.users.factories.insertMockedAdminUser
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import io.ktor.client.request.delete
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
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EventExternalLinkRoutesTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `POST external-link creates external link and returns 201`() = testApplication {
        val eventId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventSlug = "test-event"
        val orgSlug = "test-org"

        application {
            moduleMocked()
            val admin = insertMockedAdminUser()
            val org = insertMockedOrganisationEntity(id = orgId, name = orgSlug, representativeUser = admin)
            insertMockedEventWithOrga(id = eventId, slug = eventSlug, organisation = org)
            insertMockedOrgaPermission(orgId = orgId, user = admin)
        }

        val request = CreateEventExternalLinkRequest(
            name = "Call for Papers",
            url = "https://sessionize.com/devlille2025",
        )

        val response = client.post("/orgs/$orgSlug/events/$eventSlug/external-link") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(request))
        }

        assertEquals(HttpStatusCode.Created, response.status)

        val responseBody = response.bodyAsText()
        // The response should be a UUID string
        val body = responseBody.replace("\"", "") // Remove quotes from JSON string

        // Verify it has the id
        assertTrue(body.contains("id"))
    }

    @Test
    fun `POST external-link returns 400 for empty name`() = testApplication {
        val eventId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventSlug = "test-event"
        val orgSlug = "test-org"

        application {
            moduleMocked()
            val admin = insertMockedAdminUser()
            val org = insertMockedOrganisationEntity(id = orgId, name = orgSlug, representativeUser = admin)
            insertMockedEventWithOrga(id = eventId, slug = eventSlug, organisation = org)
            insertMockedOrgaPermission(orgId = orgId, user = admin)
        }

        val request = CreateEventExternalLinkRequest(
            name = "",
            url = "https://sessionize.com/devlille2025",
        )

        val response = client.post("/orgs/$orgSlug/events/$eventSlug/external-link") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(request))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("must not be empty"))
    }

    @Test
    fun `POST external-link returns 400 for empty URL`() = testApplication {
        val eventId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventSlug = "test-event"
        val orgSlug = "test-org"

        application {
            moduleMocked()
            val admin = insertMockedAdminUser()
            val org = insertMockedOrganisationEntity(id = orgId, name = orgSlug, representativeUser = admin)
            insertMockedEventWithOrga(id = eventId, slug = eventSlug, organisation = org)
            insertMockedOrgaPermission(orgId = orgId, user = admin)
        }

        val request = CreateEventExternalLinkRequest(
            name = "Call for Papers",
            url = "",
        )

        val response = client.post("/orgs/$orgSlug/events/$eventSlug/external-link") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(request))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("must not be empty"))
    }

    @Test
    fun `POST external-link returns 400 for invalid URL format`() = testApplication {
        val eventId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventSlug = "test-event"
        val orgSlug = "test-org"

        application {
            moduleMocked()
            val admin = insertMockedAdminUser()
            val org = insertMockedOrganisationEntity(id = orgId, name = orgSlug, representativeUser = admin)
            insertMockedEventWithOrga(id = eventId, slug = eventSlug, organisation = org)
            insertMockedOrgaPermission(orgId = orgId, user = admin)
        }

        val request = CreateEventExternalLinkRequest(
            name = "Call for Papers",
            url = "invalid-url",
        )

        val response = client.post("/orgs/$orgSlug/events/$eventSlug/external-link") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(request))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("must be a valid URL"))
    }

    @Test
    fun `POST external-link returns 401 without authorization`() = testApplication {
        val eventId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventSlug = "test-event"
        val orgSlug = "test-org"

        application {
            moduleMocked()
            val admin = insertMockedAdminUser()
            val org = insertMockedOrganisationEntity(id = orgId, name = orgSlug, representativeUser = admin)
            insertMockedEventWithOrga(id = eventId, slug = eventSlug, organisation = org)
        }

        val request = CreateEventExternalLinkRequest(
            name = "Call for Papers",
            url = "https://sessionize.com/devlille2025",
        )

        val response = client.post("/orgs/$orgSlug/events/$eventSlug/external-link") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(request))
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `POST external-link returns 404 for non-existent event`() = testApplication {
        val orgId = UUID.randomUUID()
        val orgSlug = "test-org"
        val eventSlug = "non-existent-event"

        application {
            moduleMocked()
            val admin = insertMockedAdminUser()
            insertMockedOrganisationEntity(id = orgId, name = orgSlug, representativeUser = admin)
            insertMockedOrgaPermission(orgId = orgId, user = admin)
        }

        val request = CreateEventExternalLinkRequest(
            name = "Call for Papers",
            url = "https://sessionize.com/devlille2025",
        )

        val response = client.post("/orgs/$orgSlug/events/$eventSlug/external-link") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(request))
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `DELETE external-link removes external link and returns 204`() = testApplication {
        val eventId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventSlug = "test-event"
        val orgSlug = "test-org"

        application {
            moduleMocked()
            val admin = insertMockedAdminUser()
            val org = insertMockedOrganisationEntity(id = orgId, name = orgSlug, representativeUser = admin)
            insertMockedEventWithOrga(id = eventId, slug = eventSlug, organisation = org)
            insertMockedOrgaPermission(orgId = orgId, user = admin)
        }

        // First create an external link
        val createRequest = CreateEventExternalLinkRequest(
            name = "Call for Papers",
            url = "https://sessionize.com/devlille2025",
        )

        val createResponse = client.post("/orgs/$orgSlug/events/$eventSlug/external-link") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(createRequest))
        }

        assertEquals(HttpStatusCode.Created, createResponse.status)
        val createdLinkId = json.decodeFromString<Map<String, String>>(createResponse.bodyAsText())["id"]

        // Then delete it
        val deleteResponse = client.delete("/orgs/$orgSlug/events/$eventSlug/external-link/$createdLinkId") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.NoContent, deleteResponse.status)
    }

    @Test
    fun `DELETE external-link returns 404 for non-existent link`() = testApplication {
        val eventId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventSlug = "test-event"
        val orgSlug = "test-org"
        val nonExistentLinkId = UUID.randomUUID().toString()

        application {
            moduleMocked()
            val admin = insertMockedAdminUser()
            val org = insertMockedOrganisationEntity(id = orgId, name = orgSlug, representativeUser = admin)
            insertMockedEventWithOrga(id = eventId, slug = eventSlug, organisation = org)
            insertMockedOrgaPermission(orgId = orgId, user = admin)
        }

        val response = client.delete("/orgs/$orgSlug/events/$eventSlug/external-link/$nonExistentLinkId") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}
