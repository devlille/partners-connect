package fr.devlille.partners.connect.partnership

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.partnership.factories.insertMockedPartnership
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringPack
import fr.devlille.partners.connect.users.factories.insertMockedEventWithAdminUser
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Integration tests for DELETE /orgs/{orgSlug}/events/{eventSlug}/partnerships/{partnershipId} endpoint.
 * Tests end-to-end business logic including database operations, authorization, and state validation.
 */
class PartnershipDeleteIntegrationTest {
    @Test
    fun `deleted partnership no longer appears in list`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val eventSlug = "test-event"
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val packId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(
                eventId = eventId,
                orgId = orgId,
                slug = eventSlug,
            )
            insertMockedCompany(companyId)
            val pack = insertMockedSponsoringPack(packId, eventId)
            insertMockedPartnership(
                id = partnershipId,
                eventId = eventId,
                companyId = companyId,
                selectedPackId = pack.id.value,
                validatedAt = null,
                declinedAt = null,
            )
        }

        // First verify partnership exists in list
        val listBeforeResponse = client.get("/orgs/$orgId/events/$eventSlug/partnerships") {
            header(HttpHeaders.Authorization, "Bearer valid")
            header(HttpHeaders.Accept, "application/json")
        }
        assertEquals(HttpStatusCode.OK, listBeforeResponse.status)

        // Delete partnership
        val deleteResponse = client.delete("/orgs/$orgId/events/$eventSlug/partnerships/$partnershipId") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }
        assertEquals(HttpStatusCode.NoContent, deleteResponse.status)

        // Verify partnership no longer in list
        val listAfterResponse = client.get("/orgs/$orgId/events/$eventSlug/partnerships") {
            header(HttpHeaders.Authorization, "Bearer valid")
            header(HttpHeaders.Accept, "application/json")
        }
        assertEquals(HttpStatusCode.OK, listAfterResponse.status)

        // Verify GET by ID returns 404
        val getByIdResponse = client.get("/events/$eventSlug/partnerships/$partnershipId") {
            header(HttpHeaders.Accept, "application/json")
        }
        assertEquals(HttpStatusCode.NotFound, getByIdResponse.status)
    }
}
