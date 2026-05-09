package fr.devlille.partners.connect.events.infrastructure.api

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.partnership.domain.PartnershipBoothLocationItem
import fr.devlille.partners.connect.partnership.factories.insertMockedPartnership
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringPack
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import fr.devlille.partners.connect.users.factories.insertMockedUser
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EventBoothPlanRouteGetTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `GET returns booth locations for non-declined partnerships`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val company1Id = UUID.randomUUID()
        val company2Id = UUID.randomUUID()
        val withLocationId = UUID.randomUUID()
        val withoutLocationId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(company1Id)
                insertMockedCompany(company2Id)
                insertMockedSponsoringPack(packId, eventId)
                insertMockedPartnership(
                    id = withLocationId,
                    eventId = eventId,
                    companyId = company1Id,
                    selectedPackId = packId,
                    boothLocation = "A-12",
                )
                insertMockedPartnership(
                    id = withoutLocationId,
                    eventId = eventId,
                    companyId = company2Id,
                    selectedPackId = packId,
                    boothLocation = null,
                )
            }
        }

        val response = client.get("/orgs/$orgId/events/$eventId/booth-plan") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val items = json.decodeFromString<List<PartnershipBoothLocationItem>>(response.bodyAsText())
        assertEquals(2, items.size)
        val withLocation = items.single { it.partnership.id == withLocationId.toString() }
        val withoutLocation = items.single { it.partnership.id == withoutLocationId.toString() }
        assertEquals("A-12", withLocation.boothLocation)
        assertNull(withoutLocation.boothLocation)
    }

    @Test
    fun `GET excludes declined partnerships`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val activeCompanyId = UUID.randomUUID()
        val declinedCompanyId = UUID.randomUUID()
        val activeId = UUID.randomUUID()
        val declinedId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(activeCompanyId)
                insertMockedCompany(declinedCompanyId)
                insertMockedSponsoringPack(packId, eventId)
                insertMockedPartnership(
                    id = activeId,
                    eventId = eventId,
                    companyId = activeCompanyId,
                    selectedPackId = packId,
                    boothLocation = "B-1",
                )
                insertMockedPartnership(
                    id = declinedId,
                    eventId = eventId,
                    companyId = declinedCompanyId,
                    selectedPackId = packId,
                    boothLocation = "B-2",
                    declinedAt = LocalDateTime(2024, 1, 1, 0, 0),
                )
            }
        }

        val response = client.get("/orgs/$orgId/events/$eventId/booth-plan") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val items = json.decodeFromString<List<PartnershipBoothLocationItem>>(response.bodyAsText())
        assertEquals(1, items.size)
        assertEquals(activeId.toString(), items.single().partnership.id)
    }

    @Test
    fun `GET returns empty list when no partnerships exist`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
            }
        }

        val response = client.get("/orgs/$orgId/events/$eventId/booth-plan") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val items = json.decodeFromString<List<PartnershipBoothLocationItem>>(response.bodyAsText())
        assertTrue(items.isEmpty())
    }

    @Test
    fun `GET returns 401 when Authorization header is missing`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
            }
        }

        val response = client.get("/orgs/$orgId/events/$eventId/booth-plan")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `GET returns 401 when user lacks organization permission`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                // NO insertMockedOrgaPermission
                insertMockedFutureEvent(eventId, orgId = orgId)
            }
        }

        val response = client.get("/orgs/$orgId/events/$eventId/booth-plan") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `GET returns 404 when event slug is unknown`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
            }
        }

        val unknownEventId = UUID.randomUUID()
        val response = client.get("/orgs/$orgId/events/$unknownEventId/booth-plan") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}
