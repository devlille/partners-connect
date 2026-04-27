package fr.devlille.partners.connect.partnership.infrastructure.api

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.partnership.factories.insertMockedBoothActivity
import fr.devlille.partners.connect.partnership.factories.insertMockedBoothOption
import fr.devlille.partners.connect.partnership.factories.insertMockedPartnership
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringPack
import io.ktor.client.request.delete
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class BoothActivityRouteDeleteTest {
    @Test
    fun `DELETE removes activity and returns 204`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val activityId = UUID.randomUUID()

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
                insertMockedBoothActivity(id = activityId, partnershipId = partnershipId)
            }
        }

        val response = client.delete("/events/$eventId/partnerships/$partnershipId/activities/$activityId")

        assertEquals(HttpStatusCode.NoContent, response.status)
    }

    @Test
    fun `DELETE returns 404 for unknown activity`() = testApplication {
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

        val response = client.delete("/events/$eventId/partnerships/$partnershipId/activities/${UUID.randomUUID()}")

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `DELETE returns 404 when activity belongs to different partnership`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val otherPartnershipId = UUID.randomUUID()
        val otherCompanyId = UUID.randomUUID()
        val activityId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedOrganisationEntity(orgId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(companyId)
                insertMockedCompany(otherCompanyId)
                insertMockedSponsoringPack(packId, eventId)
                insertMockedPartnership(
                    id = partnershipId,
                    eventId = eventId,
                    companyId = companyId,
                    selectedPackId = packId,
                )
                insertMockedPartnership(
                    id = otherPartnershipId,
                    eventId = eventId,
                    companyId = otherCompanyId,
                    selectedPackId = packId,
                )
                insertMockedBoothOption(partnershipId = partnershipId, packId = packId, eventId = eventId)
                insertMockedBoothActivity(id = activityId, partnershipId = otherPartnershipId)
            }
        }

        val response = client.delete("/events/$eventId/partnerships/$partnershipId/activities/$activityId")

        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}
