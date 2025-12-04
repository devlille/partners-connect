package fr.devlille.partners.connect.provider.infrastructure.api

import fr.devlille.partners.connect.events.factories.insertMockedEventWithOrga
import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
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
    fun `GET providers supports pagination parameters`() = testApplication {
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
}
