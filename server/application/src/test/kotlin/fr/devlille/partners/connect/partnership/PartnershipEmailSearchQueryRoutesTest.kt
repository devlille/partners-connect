package fr.devlille.partners.connect.partnership

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.partnership.factories.insertMockedPartnership
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringPack
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import fr.devlille.partners.connect.users.factories.insertMockedUser
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests that POST /orgs/{orgSlug}/events/{eventSlug}/partnerships/email respects
 * the `query` parameter (case-insensitive substring match on company name),
 * mirroring the GET listing endpoint behaviour.
 */
class PartnershipEmailSearchQueryRoutesTest {
    @Test
    fun `POST email with query that matches no company returns 404`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val companyId = UUID.randomUUID()

        application {
            moduleSharedDb(userId = userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(companyId, name = "Acme Corporation")
                insertMockedSponsoringPack(packId, eventId)
                insertMockedPartnership(eventId = eventId, companyId = companyId, selectedPackId = packId)
            }
        }

        val response = client.post("/orgs/$orgId/events/$eventId/partnerships/email?query=nonexistent") {
            header(HttpHeaders.Authorization, "Bearer valid")
            contentType(ContentType.Application.Json)
            setBody("""{"subject":"Test Subject","body":"<p>Hello</p>"}""")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}
