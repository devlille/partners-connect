package fr.devlille.partners.connect.partnership

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.partnership.domain.PartnershipItem
import fr.devlille.partners.connect.partnership.factories.insertMockedPartnership
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringPack
import fr.devlille.partners.connect.users.factories.insertMockedEventWithAdminUser
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class PartnershipListRoutesSimpleTest {
    @Test
    fun `GET returns empty array when no partnerships`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId)
        }

        val response = client.get("/orgs/$orgId/events/$eventId/partnership") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val partnerships = response.body<List<PartnershipItem>>()
        assertEquals(0, partnerships.size)
    }

    @Test
    fun `GET returns 401 when no authorization header`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId)
        }

        val response = client.get("/orgs/$orgId/events/$eventId/partnership")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `GET returns one partnership when one exists`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val packId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId)
            insertMockedCompany(companyId, "Test Company")
            val pack = insertMockedSponsoringPack(packId, eventId, "Test Pack")

            insertMockedPartnership(
                eventId = eventId,
                companyId = companyId,
                selectedPackId = pack.id.value,
                contactName = "John Doe",
                contactRole = "Manager",
            )
        }

        val response = client.get("/orgs/$orgId/events/$eventId/partnership") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val partnerships = response.body<List<PartnershipItem>>()
        assertEquals(1, partnerships.size)

        val partnership = partnerships[0]
        assertEquals("John Doe", partnership.contact.displayName)
        assertEquals("Manager", partnership.contact.role)
        assertEquals("Test Company", partnership.companyName)
        assertEquals("Test Pack", partnership.packName)
    }
}
