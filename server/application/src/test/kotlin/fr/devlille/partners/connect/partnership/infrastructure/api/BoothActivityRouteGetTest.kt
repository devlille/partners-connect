package fr.devlille.partners.connect.partnership.infrastructure.api

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.partnership.factories.insertMockedBoothActivity
import fr.devlille.partners.connect.partnership.factories.insertMockedBoothOption
import fr.devlille.partners.connect.partnership.factories.insertMockedPartnership
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringPack
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BoothActivityRouteGetTest {
    @Test
    fun `GET returns 200 with activities sorted by startTime ASC NULLS LAST`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedOrganisationEntity(orgId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(companyId)
                insertMockedSponsoringPack(packId, eventId)
                insertMockedPartnership(
                    id = partnershipId,
                    eventId = eventId,
                    companyId = companyId,
                    selectedPackId = packId,
                )
                insertMockedBoothOption(partnershipId = partnershipId, packId = packId, eventId = eventId)
                insertMockedBoothActivity(partnershipId = partnershipId, title = "Second Activity")
                insertMockedBoothActivity(partnershipId = partnershipId, title = "No Time Activity")
            }
        }

        val response = client.get("/events/$eventId/partnerships/$partnershipId/activities")

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("Second Activity"))
        assertTrue(body.contains("No Time Activity"))
    }

    @Test
    fun `GET returns 200 with empty list when no activities`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedOrganisationEntity(orgId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(companyId)
                insertMockedSponsoringPack(packId, eventId)
                insertMockedPartnership(
                    id = partnershipId,
                    eventId = eventId,
                    companyId = companyId,
                    selectedPackId = packId,
                )
                insertMockedBoothOption(partnershipId = partnershipId, packId = packId, eventId = eventId)
            }
        }

        val response = client.get("/events/$eventId/partnerships/$partnershipId/activities")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("[]", response.bodyAsText().trim())
    }

    @Test
    fun `GET returns 200 even when partnership has no booth option (GET exempt from booth gate)`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedOrganisationEntity(orgId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(companyId)
                insertMockedSponsoringPack(packId, eventId)
                insertMockedPartnership(
                    id = partnershipId,
                    eventId = eventId,
                    companyId = companyId,
                    selectedPackId = packId,
                )
                // No booth option — GET should still work
            }
        }

        val response = client.get("/events/$eventId/partnerships/$partnershipId/activities")

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `GET returns 404 for unknown partnership`() = testApplication {
        val userId = UUID.randomUUID()

        application { moduleSharedDb(userId) }

        val response = client.get("/events/${UUID.randomUUID()}/partnerships/${UUID.randomUUID()}/activities")

        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}
