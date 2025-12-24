package fr.devlille.partners.connect.events.infrastructure.api

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
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
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days

class EventCommunicationPlanRouteGetTest {
    @Test
    @Suppress("LongMethod") // Comprehensive test covering main functionality
    fun `GET communication plan returns grouped and sorted communications`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val company1Id = UUID.randomUUID()
        val company2Id = UUID.randomUUID()
        val company3Id = UUID.randomUUID()
        val packId = UUID.randomUUID()

        val partnership1Id = UUID.randomUUID()
        val partnership2Id = UUID.randomUUID()
        val partnership3Id = UUID.randomUUID()

        val now = Clock.System.now()
        val pastDate = now.minus(duration = 1.days).toLocalDateTime(TimeZone.UTC)
        val futureDate = now.plus(duration = 1.days).toLocalDateTime(TimeZone.UTC)

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(company1Id)
                insertMockedCompany(company2Id)
                insertMockedCompany(company3Id)
                insertMockedSponsoringPack(packId, eventId)
                insertMockedPartnership(
                    id = partnership1Id,
                    eventId = eventId,
                    companyId = company1Id,
                    selectedPackId = packId,
                    communicationPublicationDate = pastDate,
                    communicationSupportUrl = "https://example.com/support1.png",
                )
                insertMockedPartnership(
                    id = partnership2Id,
                    eventId = eventId,
                    companyId = company2Id,
                    selectedPackId = packId,
                    communicationPublicationDate = futureDate,
                    communicationSupportUrl = "https://example.com/support2.jpg",
                )
                insertMockedPartnership(
                    id = partnership3Id,
                    eventId = eventId,
                    companyId = company3Id,
                    selectedPackId = packId,
                    communicationPublicationDate = null,
                    communicationSupportUrl = null,
                )
            }
        }

        val response = client.get("/orgs/$orgId/events/$eventId/communication") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)

        val responseBody = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        // Check done group (past publications)
        val done = responseBody["done"]!!.jsonArray
        assertEquals(1, done.size)
        val doneItem = done[0].jsonObject
        assertEquals(partnership1Id.toString(), doneItem["partnership_id"]!!.jsonPrimitive.content)
        assertEquals(company1Id.toString(), doneItem["company_name"]!!.jsonPrimitive.content)
        assertEquals(pastDate.toString(), doneItem["publication_date"]!!.jsonPrimitive.content)
        assertEquals("https://example.com/support1.png", doneItem["support_url"]!!.jsonPrimitive.content)

        // Check planned group (future publications)
        val planned = responseBody["planned"]!!.jsonArray
        assertEquals(1, planned.size)
        val plannedItem = planned[0].jsonObject
        assertEquals(partnership2Id.toString(), plannedItem["partnership_id"]!!.jsonPrimitive.content)
        assertEquals(company2Id.toString(), plannedItem["company_name"]!!.jsonPrimitive.content)
        assertEquals(futureDate.toString(), plannedItem["publication_date"]!!.jsonPrimitive.content)
        assertEquals("https://example.com/support2.jpg", plannedItem["support_url"]!!.jsonPrimitive.content)

        // Check unplanned group (no publication date)
        val unplanned = responseBody["unplanned"]!!.jsonArray
        assertEquals(1, unplanned.size)
        val unplannedItem = unplanned[0].jsonObject
        assertEquals(partnership3Id.toString(), unplannedItem["partnership_id"]!!.jsonPrimitive.content)
        assertEquals(company3Id.toString(), unplannedItem["company_name"]!!.jsonPrimitive.content)
        assertNull(unplannedItem["publication_date"])
        assertNull(unplannedItem["support_url"])
    }

    @Test
    fun `GET communication plan sorts done group by publication date descending`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val company1Id = UUID.randomUUID()
        val company2Id = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val partnership1Id = UUID.randomUUID()
        val partnership2Id = UUID.randomUUID()

        val now = Clock.System.now()
        val olderDate = now.minus(duration = 2.days).toLocalDateTime(TimeZone.UTC)
        val newerDate = now.minus(duration = 1.days).toLocalDateTime(TimeZone.UTC)

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(company1Id, "Older Company")
                insertMockedCompany(company2Id, "Newer Company")
                insertMockedSponsoringPack(packId, eventId)

                insertMockedPartnership(
                    id = partnership1Id,
                    eventId = eventId,
                    companyId = company1Id,
                    selectedPackId = packId,
                    communicationPublicationDate = olderDate,
                )

                insertMockedPartnership(
                    id = partnership2Id,
                    eventId = eventId,
                    companyId = company2Id,
                    selectedPackId = packId,
                    communicationPublicationDate = newerDate,
                )
            }
        }

        val response = client.get("/orgs/$orgId/events/$eventId/communication") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)

        val responseBody = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        val done = responseBody["done"]!!.jsonArray
        assertEquals(2, done.size)

        // Should be sorted by publication date descending (newest first)
        assertEquals("Newer Company", done[0].jsonObject["company_name"]!!.jsonPrimitive.content)
        assertEquals("Older Company", done[1].jsonObject["company_name"]!!.jsonPrimitive.content)
    }

    @Test
    fun `GET communication plan sorts planned group by publication date ascending`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val company1Id = UUID.randomUUID()
        val company2Id = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val partnership1Id = UUID.randomUUID()
        val partnership2Id = UUID.randomUUID()

        val now = Clock.System.now()
        val earlierDate = now.plus(duration = 1.days).toLocalDateTime(TimeZone.UTC)
        val laterDate = now.plus(duration = 2.days).toLocalDateTime(TimeZone.UTC)

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(company1Id, "Later Company")
                insertMockedCompany(company2Id, "Earlier Company")
                insertMockedSponsoringPack(packId, eventId)

                insertMockedPartnership(
                    id = partnership1Id,
                    eventId = eventId,
                    companyId = company1Id,
                    selectedPackId = packId,
                    communicationPublicationDate = laterDate,
                )

                insertMockedPartnership(
                    id = partnership2Id,
                    eventId = eventId,
                    companyId = company2Id,
                    selectedPackId = packId,
                    communicationPublicationDate = earlierDate,
                )
            }
        }

        val response = client.get("/orgs/$orgId/events/$eventId/communication") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)

        val responseBody = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        val planned = responseBody["planned"]!!.jsonArray
        assertEquals(2, planned.size)

        // Should be sorted by publication date ascending (earliest first)
        assertEquals("Earlier Company", planned[0].jsonObject["company_name"]!!.jsonPrimitive.content)
        assertEquals("Later Company", planned[1].jsonObject["company_name"]!!.jsonPrimitive.content)
    }

    @Test
    fun `GET communication plan returns empty groups when no partnerships exist`() = testApplication {
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

        val response = client.get("/orgs/$orgId/events/$eventId/communication") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)

        val responseBody = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertTrue(responseBody["done"]!!.jsonArray.isEmpty())
        assertTrue(responseBody["planned"]!!.jsonArray.isEmpty())
        assertTrue(responseBody["unplanned"]!!.jsonArray.isEmpty())
    }

    @Test
    fun `GET communication plan returns 401 when unauthorized`() = testApplication {
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

        val response = client.get("/orgs/$orgId/events/$eventId/communication")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `GET communication plan returns 404 for non-existent event`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
            }
        }

        val response = client.get("/orgs/$orgId/events/$eventId/communication") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertTrue(response.bodyAsText().contains("Event with slug $eventId not found"))
    }
}
