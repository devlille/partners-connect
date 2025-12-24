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
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PartnershipBoothLocationRoutesTest {
    @Test
    fun `POST allows reassigning location to same partnership`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(companyId)
                insertMockedSponsoringPack(packId, eventId)
                insertMockedPartnership(
                    id = partnershipId,
                    eventId = eventId,
                    companyId = companyId,
                    selectedPackId = packId,
                )
            }
        }

        // First assignment
        val response1 = client.put("/orgs/$orgId/events/$eventId/partnerships/$partnershipId/booth-location") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody("""{"location": "B-5"}""")
        }

        assertEquals(HttpStatusCode.OK, response1.status)

        // Reassign to different location
        val response2 = client.put("/orgs/$orgId/events/$eventId/partnerships/$partnershipId/booth-location") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody("""{"location": "C-10"}""")
        }

        assertEquals(HttpStatusCode.OK, response2.status)
        val body = response2.bodyAsText()
        assertTrue(body.contains("C-10"))
    }

    @Test
    fun `POST returns 403 when location is already assigned to another partnership`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val companyId1 = UUID.randomUUID()
        val companyId2 = UUID.randomUUID()
        val partnershipId1 = UUID.randomUUID()
        val partnershipId2 = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(companyId1)
                insertMockedCompany(companyId2)
                insertMockedSponsoringPack(packId, eventId)
                insertMockedPartnership(
                    id = partnershipId1,
                    eventId = eventId,
                    companyId = companyId1,
                    selectedPackId = packId,
                )
                insertMockedPartnership(
                    id = partnershipId2,
                    eventId = eventId,
                    companyId = companyId2,
                    selectedPackId = packId,
                )
            }
        }

        // Assign location to first partnership
        val response1 = client.put(
            "/orgs/$orgId/events/$eventId/partnerships/$partnershipId1/booth-location",
        ) {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody("""{"location": "D-3"}""")
        }

        assertEquals(HttpStatusCode.OK, response1.status)

        // Try to assign same location to second partnership
        val response2 = client.put(
            "/orgs/$orgId/events/$eventId/partnerships/$partnershipId2/booth-location",
        ) {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody("""{"location": "D-3"}""")
        }

        assertEquals(HttpStatusCode.Forbidden, response2.status)
        val body = response2.bodyAsText()
        assertTrue(body.contains("already assigned"))
        assertTrue(body.contains("$companyId1"))
    }

    @Test
    fun `POST allows same location across different events`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId1 = UUID.randomUUID()
        val eventId2 = UUID.randomUUID()
        val packId1 = UUID.randomUUID()
        val packId2 = UUID.randomUUID()
        val companyId1 = UUID.randomUUID()
        val companyId2 = UUID.randomUUID()
        val partnershipId1 = UUID.randomUUID()
        val partnershipId2 = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId1, orgId = orgId)
                insertMockedFutureEvent(eventId2, orgId = orgId)
                insertMockedCompany(companyId1)
                insertMockedCompany(companyId2)
                insertMockedSponsoringPack(packId1, eventId1)
                insertMockedSponsoringPack(packId2, eventId2)
                insertMockedPartnership(
                    id = partnershipId1,
                    eventId = eventId1,
                    companyId = companyId1,
                    selectedPackId = packId1,
                )
                insertMockedPartnership(
                    id = partnershipId2,
                    eventId = eventId2,
                    companyId = companyId2,
                    selectedPackId = packId2,
                )
            }
        }

        // Assign location to partnership in first event
        val response1 = client.put(
            "/orgs/$orgId/events/$eventId1/partnerships/$partnershipId1/booth-location",
        ) {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody("""{"location": "E-7"}""")
        }

        assertEquals(HttpStatusCode.OK, response1.status)

        // Assign same location to partnership in second event (should succeed)
        val response2 = client.put(
            "/orgs/$orgId/events/$eventId2/partnerships/$partnershipId2/booth-location",
        ) {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody("""{"location": "E-7"}""")
        }

        assertEquals(HttpStatusCode.OK, response2.status)
        val body = response2.bodyAsText()
        assertTrue(body.contains("E-7"))
    }
}
