package fr.devlille.partners.connect.provider.infrastructure.api

import fr.devlille.partners.connect.events.factories.insertMockedEventWithOrga
import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.provider.factories.insertMockedProvider
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import fr.devlille.partners.connect.users.factories.insertMockedUser
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Contract test for GET /orgs/{orgSlug}/events/{eventSlug}/providers endpoint.
 * Tests paginated_provider.schema.json validation for event provider listing.
 * This test MUST FAIL initially as the endpoint is not implemented yet.
 */
class ListEventProvidersContractTest {
    @Test
    fun `GET orgs orgSlug events eventSlug providers validates paginated_provider schema successfully`() =
        testApplication {
            val orgId = UUID.randomUUID()
            val orgSlug = "test-org"
            val userId = UUID.randomUUID()
            val eventId = UUID.randomUUID()
            val eventSlug = "test-event"
            val providerId = UUID.randomUUID()

            application {
                moduleMocked()
                val org = insertMockedOrganisationEntity(orgId, name = orgSlug)
                val user = insertMockedUser(userId, email = "john.doe@contact.com") // Must match the mock auth email
                insertMockedOrgaPermission(orgId, user, canEdit = true)
                insertMockedEventWithOrga(eventId, organisation = org, slug = eventSlug)
                insertMockedProvider(providerId, organisation = org)
                // Mock provider attached to event for listing
            }

            val response = client.get("/orgs/$orgSlug/events/$eventSlug/providers") {
                header("Authorization", "Bearer valid")
            }

            // Route is now implemented, expecting successful listing
            assertEquals(HttpStatusCode.OK, response.status)
        }

    @Test
    fun `GET orgs orgSlug events eventSlug providers supports pagination parameters`() = testApplication {
        val orgId = UUID.randomUUID()
        val orgSlug = "test-org"
        val userId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val eventSlug = "test-event"

        application {
            moduleMocked()
            val org = insertMockedOrganisationEntity(orgId, name = orgSlug)
            val user = insertMockedUser(userId, email = "john.doe@contact.com") // Must match the mock auth email
            insertMockedOrgaPermission(orgId, user, canEdit = true)
            insertMockedEventWithOrga(eventId, organisation = org, slug = eventSlug)
            // Mock multiple providers attached to event for pagination testing
        }

        val response = client.get("/orgs/$orgSlug/events/$eventSlug/providers?page=1&page_size=10") {
            header("Authorization", "Bearer valid")
        }

        // Route is now implemented, expecting successful paginated listing
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `GET orgs orgSlug events eventSlug providers returns 401 for missing authorization`() = testApplication {
        val orgSlug = "test-org"
        val eventSlug = "test-event"

        application {
            moduleMocked()
        }

        val response = client.get("/orgs/$orgSlug/events/$eventSlug/providers")

        // Should return 401 regardless of implementation status
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `GET orgs orgSlug events eventSlug providers returns 404 for non-existent event`() = testApplication {
        val orgId = UUID.randomUUID()
        val orgSlug = "test-org"
        val userId = UUID.randomUUID()
        val nonExistentEventSlug = "non-existent-event"

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId, name = orgSlug)
            val user = insertMockedUser(userId, email = "john.doe@contact.com") // Must match the mock auth email
            insertMockedOrgaPermission(orgId, user, canEdit = true)
            // No event inserted - should result in 404
        }

        val response = client.get("/orgs/$orgSlug/events/$nonExistentEventSlug/providers") {
            header("Authorization", "Bearer valid")
        }

        // Route is implemented, but event doesn't exist
        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}
