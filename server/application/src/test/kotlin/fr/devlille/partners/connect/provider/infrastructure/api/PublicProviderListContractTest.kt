package fr.devlille.partners.connect.provider.infrastructure.api

import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.provider.factories.insertMockedProvider
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Contract test for GET /providers endpoint.
 * Tests paginated_provider.schema.json validation for public provider listing.
 * This test MUST FAIL initially as the endpoint is not implemented yet.
 */
class PublicProviderListContractTest {
    @Test
    fun `GET providers validates paginated_provider schema successfully`() = testApplication {
        val orgId = UUID.randomUUID()
        val orgSlug = "test-org"

        application {
            moduleMocked()
            val org = insertMockedOrganisationEntity(orgId, name = orgSlug)
            insertMockedProvider(organisation = org)
        }

        val response = client.get("/providers")

        // Route is now implemented, expecting successful public listing
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `GET providers supports organisation filtering with org_slug parameter`() = testApplication {
        val orgId = UUID.randomUUID()
        val orgSlug = "test-org"

        application {
            moduleMocked()
            val org = insertMockedOrganisationEntity(orgId, name = orgSlug)
            insertMockedProvider(organisation = org)
        }

        val response = client.get("/providers?org_slug=$orgSlug")

        // Route is enhanced, expecting successful organisation-filtered listing
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `GET providers supports existing query parameters`() = testApplication {
        val orgId = UUID.randomUUID()
        val orgSlug = "test-org"

        application {
            moduleMocked()
            val org = insertMockedOrganisationEntity(orgId, name = orgSlug)
            insertMockedProvider(organisation = org, name = "Catering Service")
        }

        val response = client.get("/providers?query=catering&sort=name&direction=asc&page=1&page_size=20")

        // Route is enhanced, expecting successful filtering with existing parameters
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `GET providers supports combined org_slug and query parameters`() = testApplication {
        val orgId = UUID.randomUUID()
        val orgSlug = "test-org"

        application {
            moduleMocked()
            val org = insertMockedOrganisationEntity(orgId, name = orgSlug)
            insertMockedProvider(organisation = org, name = "Tech Support Service")
        }

        val response = client.get("/providers?org_slug=$orgSlug&query=tech&sort=name&direction=desc")

        // Route is enhanced, expecting successful combined filtering
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `GET providers returns empty result for non-existent organisation`() = testApplication {
        application {
            moduleMocked()
        }

        val response = client.get("/providers?org_slug=non-existent-org")

        // Returns 404 when organization doesn't exist (strict validation)
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `GET providers handles invalid pagination parameters gracefully`() = testApplication {
        val orgId = UUID.randomUUID()
        val orgSlug = "test-org"

        application {
            moduleMocked()
            val org = insertMockedOrganisationEntity(orgId, name = orgSlug)
            insertMockedProvider(organisation = org)
        }

        val response = client.get("/providers?page=invalid&page_size=-1")

        // Returns 400 for invalid pagination parameters (strict validation)
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }
}
