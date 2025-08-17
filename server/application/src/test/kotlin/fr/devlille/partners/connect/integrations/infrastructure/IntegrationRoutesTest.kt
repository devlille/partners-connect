package fr.devlille.partners.connect.integrations.infrastructure

import fr.devlille.partners.connect.events.factories.insertMockedEvent
import fr.devlille.partners.connect.integrations.domain.CreateIntegration
import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.users.factories.insertMockedAdminUser
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
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
        assertTrue(response.bodyAsText().contains("Invalid usage"), "Expected error about usage")
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
        
        val response = client.post("/orgs/$testOrgSlug/events/$testEventSlug/integrations/invalid_provider/notification") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody("{}")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("Invalid provider"), "Expected error about provider")
    }
}
