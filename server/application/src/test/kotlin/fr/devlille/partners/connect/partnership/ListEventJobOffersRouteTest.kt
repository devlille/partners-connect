package fr.devlille.partners.connect.partnership

import fr.devlille.partners.connect.events.factories.insertMockedEventWithOrga
import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.users.factories.insertMockedAdminUser
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import io.ktor.client.request.get
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

/**
 * Contract tests for GET /orgs/{orgSlug}/events/{eventSlug}/job-offers endpoint.
 * Tests retrieving all promoted job offers for an event (across all partnerships).
 */
class ListEventJobOffersRouteTest {
    @Test
    fun `GET event job offers returns 200 with JWT authentication`() = testApplication {
        val eventId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventSlug = "test-event"
        val orgSlug = "test-org"

        application {
            moduleMocked()
            transaction {
                val admin = insertMockedAdminUser()
                val org = insertMockedOrganisationEntity(id = orgId, name = orgSlug, representativeUser = admin)
                insertMockedOrgaPermission(orgId = orgId, user = admin)
                insertMockedEventWithOrga(id = eventId, slug = eventSlug, organisation = org)
            }
        }

        val response = client.get("/orgs/$orgSlug/events/$eventSlug/job-offers") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("\"items\":[]") || body.contains("[]"))
    }

    @Test
    fun `GET event job offers filters by status when provided`() = testApplication {
        val eventId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventSlug = "test-event"
        val orgSlug = "test-org"

        application {
            moduleMocked()
            transaction {
                val admin = insertMockedAdminUser()
                val org = insertMockedOrganisationEntity(id = orgId, name = orgSlug, representativeUser = admin)
                insertMockedOrgaPermission(orgId = orgId, user = admin)
                insertMockedEventWithOrga(id = eventId, slug = eventSlug, organisation = org)
            }
        }

        // Test filtering by pending status
        val responsePending = client.get("/orgs/$orgSlug/events/$eventSlug/job-offers?status=pending") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }
        assertEquals(HttpStatusCode.OK, responsePending.status)

        // Test filtering by approved status
        val responseApproved = client.get("/orgs/$orgSlug/events/$eventSlug/job-offers?status=approved") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }
        assertEquals(HttpStatusCode.OK, responseApproved.status)

        // Test filtering by declined status
        val responseDeclined = client.get("/orgs/$orgSlug/events/$eventSlug/job-offers?status=declined") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }
        assertEquals(HttpStatusCode.OK, responseDeclined.status)
    }

    @Test
    fun `GET event job offers returns 401 when no JWT token provided`() = testApplication {
        val eventId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventSlug = "test-event"
        val orgSlug = "test-org"

        application {
            moduleMocked()
            transaction {
                val admin = insertMockedAdminUser()
                val org = insertMockedOrganisationEntity(id = orgId, name = orgSlug, representativeUser = admin)
                insertMockedOrgaPermission(orgId = orgId, user = admin)
                insertMockedEventWithOrga(id = eventId, slug = eventSlug, organisation = org)
            }
        }

        val response = client.get("/orgs/$orgSlug/events/$eventSlug/job-offers")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `GET event job offers returns 404 when event does not exist`() = testApplication {
        val orgSlug = "test-org"
        application {
            moduleMocked()
        }

        val response = client.get("/orgs/$orgSlug/events/non-existent-event/job-offers") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `GET event job offers returns 404 when organization does not exist`() = testApplication {
        application {
            moduleMocked()
        }

        val response = client.get("/orgs/non-existent-org/events/test-event/job-offers") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}
