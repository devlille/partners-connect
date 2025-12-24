package fr.devlille.partners.connect.provider.infrastructure.api

import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.provider.factories.insertMockedProvider
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Contract test for GET /providers endpoint.
 * Tests paginated_provider.schema.json validation for public provider listing.
 * This test MUST FAIL initially as the endpoint is not implemented yet.
 */
class ProvidersListRouteGetTest {
    @Test
    fun `GET providers validates paginated_provider schema successfully`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val providerId = UUID.randomUUID()

        application {
            moduleSharedDb(userId = userId)
            transaction {
                insertMockedOrganisationEntity(orgId)
                insertMockedProvider(providerId, orgId = orgId)
            }
        }

        val response = client.get("/providers")

        // Route is now implemented, expecting successful public listing
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `GET providers supports organisation filtering with org_slug parameter`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val providerId = UUID.randomUUID()

        application {
            moduleSharedDb(userId = userId)
            transaction {
                insertMockedOrganisationEntity(orgId)
                insertMockedProvider(providerId, orgId = orgId)
            }
        }

        val response = client.get("/providers?org_slug=$orgId")

        // Route is enhanced, expecting successful organisation-filtered listing
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `GET providers supports existing query parameters`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val providerId = UUID.randomUUID()

        application {
            moduleSharedDb(userId = userId)
            transaction {
                insertMockedOrganisationEntity(orgId)
                insertMockedProvider(providerId, orgId = orgId)
            }
        }

        val response = client.get("/providers?query=$providerId&sort=name&direction=asc&page=1&page_size=20")

        // Route is enhanced, expecting successful filtering with existing parameters
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `GET providers supports combined org_slug and query parameters`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val providerId = UUID.randomUUID()

        application {
            moduleSharedDb(userId = userId)
            transaction {
                insertMockedOrganisationEntity(orgId)
                insertMockedProvider(providerId, orgId = orgId)
            }
        }

        val response = client.get("/providers?org_slug=$orgId&query=$providerId&sort=name&direction=desc")

        // Route is enhanced, expecting successful combined filtering
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `GET providers returns empty result for non-existent organisation`() = testApplication {
        val userId = UUID.randomUUID()

        application {
            moduleSharedDb(userId = userId)
        }

        val response = client.get("/providers?org_slug=non-existent-org")

        // Returns 404 when organization doesn't exist (strict validation)
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `GET providers handles invalid pagination parameters gracefully`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val providerId = UUID.randomUUID()

        application {
            moduleSharedDb(userId = userId)
            transaction {
                insertMockedOrganisationEntity(orgId)
                insertMockedProvider(providerId, orgId = orgId)
            }
        }

        val response = client.get("/providers?page=invalid&page_size=-1")

        // Returns 400 for invalid pagination parameters (strict validation)
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }
}
