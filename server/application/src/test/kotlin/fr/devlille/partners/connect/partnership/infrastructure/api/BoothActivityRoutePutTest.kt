package fr.devlille.partners.connect.partnership.infrastructure.api

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.partnership.factories.insertMockedBoothActivity
import fr.devlille.partners.connect.partnership.factories.insertMockedBoothOption
import fr.devlille.partners.connect.partnership.factories.insertMockedPartnership
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringPack
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BoothActivityRoutePutTest {
    @Test
    fun `PUT updates activity with all fields and returns 200`() = testApplication {
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
                insertMockedBoothActivity(id = activityId, partnershipId = partnershipId, title = "Old Title")
            }
        }

        val response = client.put("/events/$eventId/partnerships/$partnershipId/activities/$activityId") {
            contentType(ContentType.Application.Json)
            setBody(
                """{"title":"New Title","description":"New Desc",""" +
                    """"start_time":"2026-06-14T10:00:00","end_time":"2026-06-14T11:00:00"}""",
            )
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("New Title"))
        assertTrue(body.contains("New Desc"))
    }

    @Test
    fun `PUT clears times to null and returns 200`() = testApplication {
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

        val response = client.put("/events/$eventId/partnerships/$partnershipId/activities/$activityId") {
            contentType(ContentType.Application.Json)
            setBody("""{"title":"Title","description":"Desc"}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `PUT returns 400 when startTime is after endTime`() = testApplication {
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

        val response = client.put("/events/$eventId/partnerships/$partnershipId/activities/$activityId") {
            contentType(ContentType.Application.Json)
            setBody(
                """{"title":"T","description":"D",""" +
                    """"start_time":"2026-06-14T11:00:00","end_time":"2026-06-14T10:00:00"}""",
            )
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `PUT returns 404 for unknown activity`() = testApplication {
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

        val response = client.put("/events/$eventId/partnerships/$partnershipId/activities/${UUID.randomUUID()}") {
            contentType(ContentType.Application.Json)
            setBody("""{"title":"T","description":"D"}""")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `PUT returns 404 when activity belongs to different partnership`() = testApplication {
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

        val response = client.put("/events/$eventId/partnerships/$partnershipId/activities/$activityId") {
            contentType(ContentType.Application.Json)
            setBody("""{"title":"T","description":"D"}""")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}
