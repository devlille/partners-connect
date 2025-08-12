package fr.devlille.partners.connect.integrations.infrastructure

import fr.devlille.partners.connect.integrations.domain.CreateIntegration
import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.users.factories.insertMockedEventWithAdminUser
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
        val eventId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedEventWithAdminUser(eventId)
        }

        val requestBody = Json.encodeToString(
            CreateIntegration.CreateSlackIntegration(
                token = "xoxb-test-token",
                channel = "#test",
            ),
        )

        val response = client.post("/events/$eventId/integrations/slack/notification") {
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
        val eventId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedEventWithAdminUser(eventId)
        }

        val response = client.post("/events/$eventId/integrations/slack/invalid_usage") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody("{}")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("Invalid usage"), "Expected error about usage")
    }

    @Test
    fun `POST integration - fails with unsupported provider`() = testApplication {
        val eventId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedEventWithAdminUser(eventId)
        }

        val response = client.post("/events/$eventId/integrations/invalid_provider/notification") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody("{}")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("Invalid provider"), "Expected error about provider")
    }
}
