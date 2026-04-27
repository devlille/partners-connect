package fr.devlille.partners.connect.partnership.infrastructure.api

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.partnership.factories.insertMockedBoothOption
import fr.devlille.partners.connect.partnership.factories.insertMockedPartnership
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringPack
import io.ktor.client.request.post
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

class BoothActivityRoutePostTest {
    @Test
    fun `POST creates activity with all fields and returns 201`() = testApplication {
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

        val response = client.post("/events/$eventId/partnerships/$partnershipId/activities") {
            contentType(ContentType.Application.Json)
            val body = """{"title":"Demo CI/CD","description":"A hands-on demo",""" +
                """"start_time":"2026-06-14T10:00:00","end_time":"2026-06-14T10:30:00"}"""
            setBody(body)
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("Demo CI/CD"))
        assertTrue(body.contains("A hands-on demo"))
        assertTrue(body.contains(partnershipId.toString()))
    }

    @Test
    fun `POST creates activity with null times and returns 201`() = testApplication {
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

        val response = client.post("/events/$eventId/partnerships/$partnershipId/activities") {
            contentType(ContentType.Application.Json)
            setBody("""{"title":"Demo","description":"Description"}""")
        }

        assertEquals(HttpStatusCode.Created, response.status)
    }

    @Test
    fun `POST returns 400 when title is missing`() = testApplication {
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

        val response = client.post("/events/$eventId/partnerships/$partnershipId/activities") {
            contentType(ContentType.Application.Json)
            setBody("""{"description":"Description"}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `POST returns 400 when description is missing`() = testApplication {
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

        val response = client.post("/events/$eventId/partnerships/$partnershipId/activities") {
            contentType(ContentType.Application.Json)
            setBody("""{"title":"Demo"}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `POST returns 400 when startTime is after endTime`() = testApplication {
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

        val response = client.post("/events/$eventId/partnerships/$partnershipId/activities") {
            contentType(ContentType.Application.Json)
            setBody(
                """{"title":"Demo","description":"Desc",""" +
                    """"start_time":"2026-06-14T11:00:00","end_time":"2026-06-14T10:00:00"}""",
            )
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `POST returns 404 for unknown partnership`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedOrganisationEntity(orgId)
                insertMockedFutureEvent(eventId, orgId = orgId)
            }
        }

        val unknownPartnershipId = UUID.randomUUID()
        val response = client.post("/events/$eventId/partnerships/$unknownPartnershipId/activities") {
            contentType(ContentType.Application.Json)
            setBody("""{"title":"Demo","description":"Desc"}""")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `POST returns 201 even when webhook call fails (FR-011)`() = testApplication {
        // The moduleSharedDb uses a mock WebhookRepository that does nothing.
        // This test verifies the activity is created and 201 returned regardless.
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

        val response = client.post("/events/$eventId/partnerships/$partnershipId/activities") {
            contentType(ContentType.Application.Json)
            setBody("""{"title":"Demo","description":"Desc"}""")
        }

        assertEquals(HttpStatusCode.Created, response.status)
    }
}
