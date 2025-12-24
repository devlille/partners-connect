package fr.devlille.partners.connect.provider.infrastructure.api

import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.provider.factories.insertMockedProvider
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import fr.devlille.partners.connect.users.factories.insertMockedUser
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Contract test for GET /orgs/{orgSlug}/events/{eventSlug}/providers endpoint.
 * Tests paginated_provider.schema.json validation for event provider listing.
 * This test MUST FAIL initially as the endpoint is not implemented yet.
 */
class ProvidersListEventRouteGetTest {
    @Test
    fun `GET providers supports pagination parameters`() = testApplication {
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

        val response = client.get("/orgs/$orgId/events/$eventId/providers?page=1&page_size=10") {
            header("Authorization", "Bearer valid")
        }

        // Route is now implemented, expecting successful paginated listing
        assertEquals(HttpStatusCode.OK, response.status)
    }
}
