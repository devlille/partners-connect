package fr.devlille.partners.connect.provider.infrastructure.api

import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.provider.factories.insertMockedProvider
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import fr.devlille.partners.connect.users.factories.insertMockedUser
import io.ktor.client.request.delete
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Contract test for DELETE /orgs/{orgSlug}/events/{eventSlug}/providers endpoint.
 * Tests JSON schema validation for create_by_identifiers.schema.json.
 * This test MUST FAIL initially as the endpoint is not implemented yet.
 */
class ProvidersDetachRouteDeleteTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `DELETE providers validates create_by_identifiers schema successfully`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val providerId = UUID.randomUUID()

        application {
            moduleSharedDb(userId = userId)
            transaction {
                insertMockedOrganisationEntity(orgId)
                insertMockedUser(userId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedProvider(providerId, orgId = orgId)
            }
        }

        val validDetachRequest = listOf(providerId.toString())

        val response = client.delete("/orgs/$orgId/events/$eventId/providers") {
            header("Authorization", "Bearer valid")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(validDetachRequest))
        }

        // Route is now implemented, expecting successful detachment
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `DELETE providers accepts multiple provider IDs`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val providerId1 = UUID.randomUUID()
        val providerId2 = UUID.randomUUID()

        application {
            moduleSharedDb(userId = userId)
            transaction {
                insertMockedOrganisationEntity(orgId)
                insertMockedUser(userId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedProvider(providerId1, orgId = orgId)
                insertMockedProvider(providerId2, orgId = orgId)
            }
        }

        val bulkDetachRequest = listOf(providerId1.toString(), providerId2.toString())

        val response = client.delete("/orgs/$orgId/events/$eventId/providers") {
            header("Authorization", "Bearer valid")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(bulkDetachRequest))
        }

        // Route is now implemented, expecting successful bulk detachment
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `DELETE providers returns 401 for missing authorization`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val providerId = UUID.randomUUID()

        application {
            moduleSharedDb(userId = userId)
            transaction {
                insertMockedOrganisationEntity(orgId)
                insertMockedUser(userId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedProvider(providerId, orgId = orgId)
            }
        }

        val detachRequest = listOf(providerId.toString())

        val response = client.delete("/orgs/$orgId/events/$eventId/providers") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(detachRequest))
        }

        // Should return 401 regardless of implementation status
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `DELETE providers returns 400 for invalid request body`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val providerId = UUID.randomUUID()

        application {
            moduleSharedDb(userId = userId)
            transaction {
                insertMockedOrganisationEntity(orgId)
                insertMockedUser(userId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedProvider(providerId, orgId = orgId)
            }
        }

        // Invalid request - empty array violates minItems: 1
        val invalidDetachRequest = emptyList<String>()

        val response = client.delete("/orgs/$orgId/events/$eventId/providers") {
            header("Authorization", "Bearer valid")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(invalidDetachRequest))
        }

        // Should return 400 for schema validation error
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }
}
