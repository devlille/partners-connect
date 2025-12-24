package fr.devlille.partners.connect.provider.infrastructure.api

import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.provider.domain.UpdateProvider
import fr.devlille.partners.connect.provider.factories.insertMockedProvider
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import fr.devlille.partners.connect.users.factories.insertMockedUser
import io.ktor.client.request.header
import io.ktor.client.request.put
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
 * Contract test for PUT /orgs/{orgSlug}/providers/{id} endpoint.
 * Tests JSON schema validation for update_provider.schema.json.
 * This test MUST FAIL initially as the endpoint is not implemented yet.
 */
class ProvidersUpdateRoutePutTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `PUT providers id validates update_provider schema successfully`() = testApplication {
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

        val validUpdateRequest = UpdateProvider(
            name = "Updated Provider Name",
            phone = "+33987654321",
        )

        val response = client.put("/orgs/$orgId/providers/$providerId") {
            header("Authorization", "Bearer valid")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(UpdateProvider.serializer(), validUpdateRequest))
        }

        // Route is now implemented, expecting successful update
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `PUT providers id accepts partial updates`() = testApplication {
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

        // Partial update - only name field
        val partialUpdateRequest = UpdateProvider(name = "New Name Only")

        val response = client.put("/orgs/$orgId/providers/$providerId") {
            header("Authorization", "Bearer valid")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(UpdateProvider.serializer(), partialUpdateRequest))
        }

        // Route is now implemented, expecting successful update
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `PUT providers id returns 401 for missing authorization`() = testApplication {
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

        val response = client.put("/orgs/$orgId/providers/$providerId") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(UpdateProvider.serializer(), UpdateProvider()))
        }

        // Should return 401 regardless of implementation status
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `PUT providers id returns 404 for non-existent provider`() = testApplication {
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
            }
        }

        val validUpdateRequest = UpdateProvider(name = "Updated Name")

        val response = client.put("/orgs/$orgId/providers/$providerId") {
            header("Authorization", "Bearer valid")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(UpdateProvider.serializer(), validUpdateRequest))
        }

        // Route is implemented, but provider doesn't exist
        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}
