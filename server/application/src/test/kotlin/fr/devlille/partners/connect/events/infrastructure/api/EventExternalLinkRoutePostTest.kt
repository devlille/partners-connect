package fr.devlille.partners.connect.events.infrastructure.api

import fr.devlille.partners.connect.events.domain.CreateEventExternalLinkRequest
import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import fr.devlille.partners.connect.users.factories.insertMockedUser
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
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EventExternalLinkRoutePostTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `POST external-link creates external link and returns 201`() = testApplication {
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

        val request = CreateEventExternalLinkRequest(
            name = "Call for Papers",
            url = "https://sessionize.com/devlille2025",
        )

        val response = client.post("/orgs/$orgId/events/$eventId/external-link") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(request))
        }

        assertEquals(HttpStatusCode.Created, response.status)

        val body = response
            .bodyAsText()
            .replace(oldValue = "\"", newValue = "")
        assertTrue(body.contains("id"))
    }

    @Test
    fun `POST external-link returns 400 for empty name`() = testApplication {
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

        val request = CreateEventExternalLinkRequest(
            name = "",
            url = "https://sessionize.com/devlille2025",
        )

        val response = client.post("/orgs/$orgId/events/$eventId/external-link") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(request))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("must not be empty"))
    }

    @Test
    fun `POST external-link returns 400 for empty URL`() = testApplication {
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

        val request = CreateEventExternalLinkRequest(
            name = "Call for Papers",
            url = "",
        )

        val response = client.post("/orgs/$orgId/events/$eventId/external-link") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(request))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("value does not match 'uri' format"))
    }

    @Test
    fun `POST external-link returns 400 for invalid URL format`() = testApplication {
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

        val request = CreateEventExternalLinkRequest(
            name = "Call for Papers",
            url = "invalid-url",
        )

        val response = client.post("/orgs/$orgId/events/$eventId/external-link") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(request))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("value does not match 'uri' format"))
    }

    @Test
    fun `POST external-link returns 401 without authorization`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()

        application {
            moduleSharedDb(userId = userId)
            transaction {
                insertMockedOrganisationEntity(orgId)
                insertMockedFutureEvent(eventId, orgId = orgId)
            }
        }

        val request = CreateEventExternalLinkRequest(
            name = "Call for Papers",
            url = "https://sessionize.com/devlille2025",
        )

        val response = client.post("/orgs/$orgId/events/$eventId/external-link") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(request))
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `POST external-link returns 404 for non-existent event`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()

        application {
            moduleSharedDb(userId = userId)
            transaction {
                insertMockedOrganisationEntity(orgId)
                insertMockedUser(userId)
                insertMockedOrgaPermission(orgId, userId = userId)
            }
        }

        val request = CreateEventExternalLinkRequest(
            name = "Call for Papers",
            url = "https://sessionize.com/devlille2025",
        )

        val response = client.post("/orgs/$orgId/events/event-not-exist/external-link") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(request))
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}
