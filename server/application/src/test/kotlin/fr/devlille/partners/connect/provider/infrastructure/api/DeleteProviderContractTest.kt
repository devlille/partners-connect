package fr.devlille.partners.connect.provider.infrastructure.api

import fr.devlille.partners.connect.events.factories.insertMockedEventWithOrga
import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.provider.factories.insertMockedProvider
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import fr.devlille.partners.connect.users.factories.insertMockedUser
import io.ktor.client.request.delete
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Contract test for DELETE /orgs/{orgSlug}/providers/{id} endpoint.
 * Tests error response validation for delete_provider operations.
 * This test MUST FAIL initially as the endpoint is not implemented yet.
 */
class DeleteProviderContractTest {
    @Test
    fun `DELETE orgs orgSlug providers id returns 204 for successful deletion`() = testApplication {
        val orgId = UUID.randomUUID()
        val orgSlug = "test-org"
        val userId = UUID.randomUUID()
        val providerId = UUID.randomUUID()

        application {
            moduleMocked()
            val org = insertMockedOrganisationEntity(orgId, name = orgSlug)
            val user = insertMockedUser(userId, email = "john.doe@contact.com") // Must match the mock auth email
            insertMockedOrgaPermission(orgId, user, canEdit = true)
            insertMockedProvider(providerId, organisation = org)
        }

        val response = client.delete("/orgs/$orgSlug/providers/$providerId") {
            header("Authorization", "Bearer valid")
        }

        // Route is now implemented, expecting successful deletion
        assertEquals(HttpStatusCode.NoContent, response.status)
    }

    @Test
    fun `DELETE orgs orgSlug providers id returns 409 when provider attached to events`() = testApplication {
        val orgId = UUID.randomUUID()
        val orgSlug = "test-org"
        val userId = UUID.randomUUID()
        val providerId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val eventSlug = "test-event"
        val json = Json { ignoreUnknownKeys = true }

        application {
            moduleMocked()
            val org = insertMockedOrganisationEntity(orgId, name = orgSlug)
            val user = insertMockedUser(userId, email = "john.doe@contact.com") // Must match the mock auth email
            insertMockedOrgaPermission(orgId, user, canEdit = true)
            insertMockedProvider(providerId, organisation = org)
            insertMockedEventWithOrga(eventId, organisation = org, slug = eventSlug, name = "Test Event")
        }

        // First attach the provider to an event
        val attachRequest = listOf(providerId.toString())
        val attachResponse = client.post("/orgs/$orgSlug/events/$eventSlug/providers") {
            header("Authorization", "Bearer valid")
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(attachRequest))
        }
        assertEquals(HttpStatusCode.OK, attachResponse.status)

        // Now try to delete the provider - should fail due to attachments
        val response = client.delete("/orgs/$orgSlug/providers/$providerId") {
            header("Authorization", "Bearer valid")
        }

        // Route is implemented, expecting conflict due to event attachments
        assertEquals(HttpStatusCode.Conflict, response.status)
    }

    @Test
    fun `DELETE orgs orgSlug providers id returns 401 for missing authorization`() = testApplication {
        val orgSlug = "test-org"
        val providerId = UUID.randomUUID()

        application {
            moduleMocked()
        }

        val response = client.delete("/orgs/$orgSlug/providers/$providerId")

        // Should return 401 regardless of implementation status
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `DELETE orgs orgSlug providers id returns 404 for non-existent provider`() = testApplication {
        val orgId = UUID.randomUUID()
        val orgSlug = "test-org"
        val userId = UUID.randomUUID()
        val nonExistentProviderId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId, name = orgSlug)
            val user = insertMockedUser(userId, email = "john.doe@contact.com") // Must match the mock auth email
            insertMockedOrgaPermission(orgId, user, canEdit = true)
            // No provider inserted - should result in 404
        }

        val response = client.delete("/orgs/$orgSlug/providers/$nonExistentProviderId") {
            header("Authorization", "Bearer valid")
        }

        // Route is implemented, but provider doesn't exist
        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}
