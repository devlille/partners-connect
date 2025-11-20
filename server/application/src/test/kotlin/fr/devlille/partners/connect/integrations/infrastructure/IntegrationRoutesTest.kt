package fr.devlille.partners.connect.integrations.infrastructure

import fr.devlille.partners.connect.events.factories.insertMockedEvent
import fr.devlille.partners.connect.integrations.domain.CreateIntegration
import fr.devlille.partners.connect.integrations.domain.IntegrationProvider
import fr.devlille.partners.connect.integrations.domain.IntegrationUsage
import fr.devlille.partners.connect.integrations.factories.insertMockedIntegration
import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.users.factories.insertMockedAdminUser
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
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IntegrationRoutesTest {
    @Test
    fun `POST integration - register Slack integration successfully`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val testOrgSlug = "test-org-1"
        val testEventSlug = "test-event-1"

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId, name = testOrgSlug)
            insertMockedEvent(eventId, orgId = orgId, slug = testEventSlug, name = "Test Event 1")
            insertMockedOrgaPermission(orgId = orgId, user = insertMockedAdminUser())
        }

        val requestBody = Json.encodeToString(
            CreateIntegration.CreateSlackIntegration(
                token = "xoxb-test-token",
                channel = "#test",
            ),
        )

        val response = client.post("/orgs/$testOrgSlug/events/$testEventSlug/integrations/slack/notification") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(requestBody)
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val responseBody = response.bodyAsText()
        assertTrue(responseBody.contains("id"), "Expected integration id in response")
    }

    @Test
    fun `POST integration - fails with invalid usage`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val testOrgSlug = "test-org-2"
        val testEventSlug = "test-event-2"

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId, name = testOrgSlug)
            insertMockedEvent(eventId, orgId = orgId, slug = testEventSlug, name = "Test Event 2")
            insertMockedOrgaPermission(orgId = orgId, user = insertMockedAdminUser())
        }

        val response = client.post("/orgs/$testOrgSlug/events/$testEventSlug/integrations/slack/invalid_usage") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody("{}")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(
            actual = response.bodyAsText()
                .contains("Request parameter usage couldn't be parsed/converted to IntegrationUsage"),
            message = "Expected error about usage",
        )
    }

    @Test
    fun `POST integration - fails with unsupported provider`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val testOrgSlug = "test-org-3"
        val testEventSlug = "test-event-3"

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId, name = testOrgSlug)
            insertMockedEvent(eventId, orgId = orgId, slug = testEventSlug, name = "Test Event 3")
            insertMockedOrgaPermission(orgId = orgId, user = insertMockedAdminUser())
        }

        val integrationPath = "/orgs/$testOrgSlug/events/$testEventSlug/integrations/invalid_provider/notification"
        val response = client.post(integrationPath) {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody("{}")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(
            actual = response.bodyAsText()
                .contains("Request parameter provider couldn't be parsed/converted to IntegrationProvider"),
            message = "Expected error about provider",
        )
    }

    @Test
    fun `GET integrations - returns empty list when no integrations exist`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val testOrgSlug = "test-org-4"
        val testEventSlug = "test-event-4"

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId, name = testOrgSlug)
            insertMockedEvent(eventId, orgId = orgId, slug = testEventSlug, name = "Test Event 4")
            insertMockedOrgaPermission(orgId = orgId, user = insertMockedAdminUser())
        }

        val response = client.get("/orgs/$testOrgSlug/events/$testEventSlug/integrations") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody = response.bodyAsText()
        val integrations = Json.parseToJsonElement(responseBody).jsonArray
        assertTrue(integrations.isEmpty(), "Expected empty list of integrations")
    }

    @Test
    fun `GET integrations - returns integrations for event successfully`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val testOrgSlug = "test-org-5"
        val testEventSlug = "test-event-5"

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId, name = testOrgSlug)
            insertMockedEvent(eventId, orgId = orgId, slug = testEventSlug, name = "Test Event 5")
            insertMockedOrgaPermission(orgId = orgId, user = insertMockedAdminUser())

            // Add some integrations
            insertMockedIntegration(
                eventId = eventId,
                provider = IntegrationProvider.SLACK,
                usage = IntegrationUsage.NOTIFICATION,
            )
            insertMockedIntegration(
                eventId = eventId,
                provider = IntegrationProvider.MAILJET,
                usage = IntegrationUsage.NOTIFICATION,
            )
        }

        val response = client.get("/orgs/$testOrgSlug/events/$testEventSlug/integrations") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody = response.bodyAsText()
        val integrations = Json.parseToJsonElement(responseBody).jsonArray
        assertEquals(2, integrations.size, "Expected 2 integrations")

        val integrationsData = integrations.map { it.jsonObject }
        assertTrue(integrationsData.any { it["provider"]?.toString()?.contains("slack") == true })
        assertTrue(integrationsData.any { it["provider"]?.toString()?.contains("mailjet") == true })
    }

    @Test
    fun `GET integrations - returns 404 for non-existent organization`() = testApplication {
        val testOrgSlug = "non-existent-org"
        val testEventSlug = "test-event-6"

        application {
            moduleMocked()
        }

        val response = client.get("/orgs/$testOrgSlug/events/$testEventSlug/integrations") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertTrue(response.bodyAsText().contains("not found"), "Expected not found error")
    }

    @Test
    fun `GET integrations - returns 404 for non-existent event`() = testApplication {
        val orgId = UUID.randomUUID()
        val testOrgSlug = "test-org-7"
        val testEventSlug = "non-existent-event"

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId, name = testOrgSlug)
            insertMockedOrgaPermission(orgId = orgId, user = insertMockedAdminUser())
        }

        val response = client.get("/orgs/$testOrgSlug/events/$testEventSlug/integrations") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertTrue(response.bodyAsText().contains("not found"), "Expected not found error")
    }

    @Test
    fun `GET integrations - returns 401 for unauthorized user`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val testOrgSlug = "test-org-8"
        val testEventSlug = "test-event-8"
        val email = "john.doe@contact.com" // Must match the mock auth email

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId, name = testOrgSlug)
            insertMockedEvent(eventId, orgId = orgId, slug = testEventSlug, name = "Test Event 8")
            // Create user but don't grant permission
            insertMockedUser(email = email)
        }

        val response = client.get("/orgs/$testOrgSlug/events/$testEventSlug/integrations") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `DELETE integration - deletes integration successfully`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val integrationId = UUID.randomUUID()
        val testOrgSlug = "test-org-9"
        val testEventSlug = "test-event-9"

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId, name = testOrgSlug)
            insertMockedEvent(eventId, orgId = orgId, slug = testEventSlug, name = "Test Event 9")
            insertMockedOrgaPermission(orgId = orgId, user = insertMockedAdminUser())

            // Add integration to delete
            insertMockedIntegration(
                id = integrationId,
                eventId = eventId,
                provider = IntegrationProvider.SLACK,
                usage = IntegrationUsage.NOTIFICATION,
            )
        }

        val response = client.delete(
            "/orgs/$testOrgSlug/events/$testEventSlug/integrations/$integrationId",
        ) {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.NoContent, response.status)
        assertEquals("", response.bodyAsText())
    }

    @Test
    fun `DELETE integration - returns 404 for non-existent integration`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val testOrgSlug = "test-org-10"
        val testEventSlug = "test-event-10"
        val nonExistentId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId, name = testOrgSlug)
            insertMockedEvent(eventId, orgId = orgId, slug = testEventSlug, name = "Test Event 10")
            insertMockedOrgaPermission(orgId = orgId, user = insertMockedAdminUser())
        }

        val response = client.delete(
            "/orgs/$testOrgSlug/events/$testEventSlug/integrations/$nonExistentId",
        ) {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertTrue(response.bodyAsText().contains("not found"), "Expected not found error")
    }

    @Test
    fun `DELETE integration - returns 400 for invalid integration ID format`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val testOrgSlug = "test-org-11"
        val testEventSlug = "test-event-11"
        val invalidId = "invalid-uuid"

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId, name = testOrgSlug)
            insertMockedEvent(eventId, orgId = orgId, slug = testEventSlug, name = "Test Event 11")
            insertMockedOrgaPermission(orgId = orgId, user = insertMockedAdminUser())
        }

        val response = client.delete(
            "/orgs/$testOrgSlug/events/$testEventSlug/integrations/$invalidId",
        ) {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(
            actual = response.bodyAsText().contains("Request parameter id couldn't be parsed/converted to UUID"),
            message = "Expected invalid UUID format error",
        )
    }

    @Test
    fun `DELETE integration - returns 404 for non-existent organization`() = testApplication {
        val testOrgSlug = "non-existent-org"
        val testEventSlug = "test-event-12"
        val integrationId = UUID.randomUUID()

        application {
            moduleMocked()
        }

        val response = client.delete(
            "/orgs/$testOrgSlug/events/$testEventSlug/integrations/$integrationId",
        ) {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertTrue(response.bodyAsText().contains("not found"), "Expected not found error")
    }

    @Test
    fun `DELETE integration - returns 401 for unauthorized user`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val integrationId = UUID.randomUUID()
        val testOrgSlug = "test-org-13"
        val testEventSlug = "test-event-13"
        val email = "john.doe@contact.com" // Must match the mock auth email

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId, name = testOrgSlug)
            insertMockedEvent(eventId, orgId = orgId, slug = testEventSlug, name = "Test Event 13")
            // Create user but don't grant permission
            insertMockedUser(email = email)

            insertMockedIntegration(
                id = integrationId,
                eventId = eventId,
                provider = IntegrationProvider.SLACK,
                usage = IntegrationUsage.NOTIFICATION,
            )
        }

        val response = client.delete(
            "/orgs/$testOrgSlug/events/$testEventSlug/integrations/$integrationId",
        ) {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `DELETE integration - returns 404 for integration from different event`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId1 = UUID.randomUUID()
        val eventId2 = UUID.randomUUID()
        val integrationId = UUID.randomUUID()
        val testOrgSlug = "test-org-14"
        val testEventSlug1 = "test-event-14a"
        val testEventSlug2 = "test-event-14b"

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId, name = testOrgSlug)
            insertMockedEvent(eventId1, orgId = orgId, slug = testEventSlug1, name = "Test Event 14a")
            insertMockedEvent(eventId2, orgId = orgId, slug = testEventSlug2, name = "Test Event 14b")
            insertMockedOrgaPermission(orgId = orgId, user = insertMockedAdminUser())

            // Add integration to event2, but try to delete from event1
            insertMockedIntegration(
                id = integrationId,
                eventId = eventId2,
                provider = IntegrationProvider.SLACK,
                usage = IntegrationUsage.NOTIFICATION,
            )
        }

        val response = client.delete(
            "/orgs/$testOrgSlug/events/$testEventSlug1/integrations/$integrationId",
        ) {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertTrue(response.bodyAsText().contains("not found"), "Expected not found error")
    }

    @Test
    fun `POST integration - fails when duplicate integration exists with same provider and usage`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val testOrgSlug = "test-org-15"
        val testEventSlug = "test-event-15"

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId, name = testOrgSlug)
            insertMockedEvent(eventId, orgId = orgId, slug = testEventSlug, name = "Test Event 15")
            insertMockedOrgaPermission(orgId = orgId, user = insertMockedAdminUser())

            // Add existing Slack integration with NOTIFICATION usage
            insertMockedIntegration(
                eventId = eventId,
                provider = IntegrationProvider.SLACK,
                usage = IntegrationUsage.NOTIFICATION,
            )
        }

        val requestBody = Json.encodeToString(
            CreateIntegration.CreateSlackIntegration(
                token = "xoxb-test-token-2",
                channel = "#test-2",
            ),
        )

        val response = client.post("/orgs/$testOrgSlug/events/$testEventSlug/integrations/slack/notification") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(requestBody)
        }

        assertEquals(HttpStatusCode.Conflict, response.status)
        assertTrue(
            actual = response.bodyAsText().contains("already exists"),
            message = "Expected conflict error about existing integration",
        )
    }
}
