package fr.devlille.partners.connect.integrations.infrastructure.api

import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.integrations.domain.CreateIntegration
import fr.devlille.partners.connect.integrations.domain.IntegrationProvider
import fr.devlille.partners.connect.integrations.domain.IntegrationUsage
import fr.devlille.partners.connect.integrations.factories.insertMockedIntegration
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

class IntegrationRoutePostTest {
    @Test
    fun `POST integration - register Slack integration successfully`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
            }
        }

        val requestBody = Json.encodeToString(
            CreateIntegration.CreateSlackIntegration(
                token = "xoxb-test-token",
                channel = "#test",
            ),
        )

        val response = client.post("/orgs/$orgId/events/$eventId/integrations/slack/notification") {
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
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
            }
        }

        val response = client.post("/orgs/$orgId/events/$eventId/integrations/slack/invalid_usage") {
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
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
            }
        }

        val integrationPath = "/orgs/$orgId/events/$eventId/integrations/invalid_provider/notification"
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
    fun `POST integration - fails when duplicate integration exists with same provider and usage`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)

                // Add existing Slack integration with NOTIFICATION usage
                insertMockedIntegration(
                    eventId = eventId,
                    provider = IntegrationProvider.SLACK,
                    usage = IntegrationUsage.NOTIFICATION,
                )
            }
        }

        val requestBody = Json.encodeToString(
            CreateIntegration.CreateSlackIntegration(
                token = "xoxb-test-token-2",
                channel = "#test-2",
            ),
        )

        val response = client.post("/orgs/$orgId/events/$eventId/integrations/slack/notification") {
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
