package fr.devlille.partners.connect.integrations.infrastructure.api

import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.integrations.domain.IntegrationProvider
import fr.devlille.partners.connect.integrations.domain.IntegrationUsage
import fr.devlille.partners.connect.integrations.factories.insertMockedIntegration
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import fr.devlille.partners.connect.users.factories.insertMockedUser
import io.ktor.client.request.delete
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IntegrationRouteDeleteTest {
    @Test
    fun `DELETE integration - deletes integration successfully`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val integrationId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)

                // Add existing Slack integration with NOTIFICATION usage
                insertMockedIntegration(
                    id = integrationId,
                    eventId = eventId,
                    provider = IntegrationProvider.SLACK,
                    usage = IntegrationUsage.NOTIFICATION,
                )
            }
        }

        val response = client.delete(
            "/orgs/$orgId/events/$eventId/integrations/$integrationId",
        ) {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.NoContent, response.status)
        assertEquals("", response.bodyAsText())
    }

    @Test
    fun `DELETE integration - returns 404 for non-existent integration`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val integrationId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
            }
        }

        val response = client.delete("/orgs/$orgId/events/$eventId/integrations/$integrationId") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertTrue(response.bodyAsText().contains("not found"), "Expected not found error")
    }

    @Test
    fun `DELETE integration - returns 400 for invalid integration ID format`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val integrationId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)

                // Add existing Slack integration with NOTIFICATION usage
                insertMockedIntegration(
                    id = integrationId,
                    eventId = eventId,
                    provider = IntegrationProvider.SLACK,
                    usage = IntegrationUsage.NOTIFICATION,
                )
            }
        }

        val response = client.delete("/orgs/$orgId/events/$eventId/integrations/invalid-uuid") {
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
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val integrationId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
            }
        }

        val response = client.delete("/orgs/$orgId/events/$eventId/integrations/$integrationId") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertTrue(response.bodyAsText().contains("not found"), "Expected not found error")
    }

    @Test
    fun `DELETE integration - returns 401 for unauthorized user`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val integrationId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedFutureEvent(eventId, orgId = orgId)

                // Add existing Slack integration with NOTIFICATION usage
                insertMockedIntegration(
                    id = integrationId,
                    eventId = eventId,
                    provider = IntegrationProvider.SLACK,
                    usage = IntegrationUsage.NOTIFICATION,
                )
            }
        }

        val response = client.delete("/orgs/$orgId/events/$eventId/integrations/$integrationId") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}
