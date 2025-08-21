package fr.devlille.partners.connect.partnership

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.partnership.factories.insertMockedPartnership
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringPack
import fr.devlille.partners.connect.users.factories.insertMockedEventWithAdminUser
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EventCommunicationPlanRoutesTest {

    @Test
    fun `GET communication plan returns grouped and sorted communications`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val eventSlug = "test-event-communication-plan"
        
        val company1Id = UUID.randomUUID()
        val company2Id = UUID.randomUUID()
        val company3Id = UUID.randomUUID()
        val packId = UUID.randomUUID()
        
        val partnership1Id = UUID.randomUUID()
        val partnership2Id = UUID.randomUUID()
        val partnership3Id = UUID.randomUUID()

        // Set up test data with different communication states
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val pastDate = LocalDateTime(2023, 5, 15, 10, 30, 0)
        val futureDate = LocalDateTime(2030, 12, 25, 15, 45, 0)

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)
            insertMockedCompany(company1Id, "Alpha Company")
            insertMockedCompany(company2Id, "Beta Company") 
            insertMockedCompany(company3Id, "Charlie Company")
            insertMockedSponsoringPack(packId, eventId, "Gold Pack")
            
            // Partnership with past publication date (done)
            insertMockedPartnership(
                id = partnership1Id,
                eventId = eventId,
                companyId = company1Id,
                selectedPackId = packId,
                communicationPublicationDate = pastDate,
                communicationSupportUrl = "https://example.com/support1.png"
            )
            
            // Partnership with future publication date (planned)
            insertMockedPartnership(
                id = partnership2Id,
                eventId = eventId,
                companyId = company2Id,
                selectedPackId = packId,
                communicationPublicationDate = futureDate,
                communicationSupportUrl = "https://example.com/support2.jpg"
            )
            
            // Partnership with no publication date (unplanned)
            insertMockedPartnership(
                id = partnership3Id,
                eventId = eventId,
                companyId = company3Id,
                selectedPackId = packId,
                communicationPublicationDate = null,
                communicationSupportUrl = null
            )
        }

        val response = client.get("/orgs/$orgId/events/$eventSlug/communication") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)

        val responseBody = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        
        // Check done group (past publications)
        val done = responseBody["done"]!!.jsonArray
        assertEquals(1, done.size)
        val doneItem = done[0].jsonObject
        assertEquals(partnership1Id.toString(), doneItem["partnership_id"]!!.jsonPrimitive.content)
        assertEquals("Alpha Company", doneItem["company_name"]!!.jsonPrimitive.content)
        assertEquals("2023-05-15T10:30", doneItem["publication_date"]!!.jsonPrimitive.content)
        assertEquals("https://example.com/support1.png", doneItem["support_url"]!!.jsonPrimitive.content)
        
        // Check planned group (future publications)
        val planned = responseBody["planned"]!!.jsonArray
        assertEquals(1, planned.size)
        val plannedItem = planned[0].jsonObject
        assertEquals(partnership2Id.toString(), plannedItem["partnership_id"]!!.jsonPrimitive.content)
        assertEquals("Beta Company", plannedItem["company_name"]!!.jsonPrimitive.content)
        assertEquals("2025-12-25T15:45", plannedItem["publication_date"]!!.jsonPrimitive.content)
        assertEquals("https://example.com/support2.jpg", plannedItem["support_url"]!!.jsonPrimitive.content)
        
        // Check unplanned group (no publication date)
        val unplanned = responseBody["unplanned"]!!.jsonArray
        assertEquals(1, unplanned.size)
        val unplannedItem = unplanned[0].jsonObject
        assertEquals(partnership3Id.toString(), unplannedItem["partnership_id"]!!.jsonPrimitive.content)
        assertEquals("Charlie Company", unplannedItem["company_name"]!!.jsonPrimitive.content)
        assertNull(unplannedItem["publication_date"])
        assertNull(unplannedItem["support_url"])
    }

    @Test
    fun `GET communication plan sorts done group by publication date descending`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val eventSlug = "test-event-done-sorting"
        
        val company1Id = UUID.randomUUID()
        val company2Id = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val partnership1Id = UUID.randomUUID()
        val partnership2Id = UUID.randomUUID()

        val olderDate = LocalDateTime(2023, 3, 10, 9, 0, 0)
        val newerDate = LocalDateTime(2023, 6, 20, 14, 30, 0)

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)
            insertMockedCompany(company1Id, "Older Company")
            insertMockedCompany(company2Id, "Newer Company")
            insertMockedSponsoringPack(packId, eventId, "Pack")
            
            insertMockedPartnership(
                id = partnership1Id,
                eventId = eventId,
                companyId = company1Id,
                selectedPackId = packId,
                communicationPublicationDate = olderDate
            )
            
            insertMockedPartnership(
                id = partnership2Id,
                eventId = eventId,
                companyId = company2Id,
                selectedPackId = packId,
                communicationPublicationDate = newerDate
            )
        }

        val response = client.get("/orgs/$orgId/events/$eventSlug/communication") {
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
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val eventSlug = "test-event-planned-sorting"
        
        val company1Id = UUID.randomUUID()
        val company2Id = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val partnership1Id = UUID.randomUUID()
        val partnership2Id = UUID.randomUUID()

        val earlierDate = LocalDateTime(2030, 6, 15, 10, 0, 0)
        val laterDate = LocalDateTime(2030, 9, 30, 16, 30, 0)

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)
            insertMockedCompany(company1Id, "Later Company")
            insertMockedCompany(company2Id, "Earlier Company")
            insertMockedSponsoringPack(packId, eventId, "Pack")
            
            insertMockedPartnership(
                id = partnership1Id,
                eventId = eventId,
                companyId = company1Id,
                selectedPackId = packId,
                communicationPublicationDate = laterDate
            )
            
            insertMockedPartnership(
                id = partnership2Id,
                eventId = eventId,
                companyId = company2Id,
                selectedPackId = packId,
                communicationPublicationDate = earlierDate
            )
        }

        val response = client.get("/orgs/$orgId/events/$eventSlug/communication") {
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
    fun `GET communication plan sorts unplanned group by company name ascending`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val eventSlug = "test-event-unplanned-sorting"
        
        val company1Id = UUID.randomUUID()
        val company2Id = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val partnership1Id = UUID.randomUUID()
        val partnership2Id = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)
            insertMockedCompany(company1Id, "Zebra Company")
            insertMockedCompany(company2Id, "Alpha Company")
            insertMockedSponsoringPack(packId, eventId, "Pack")
            
            insertMockedPartnership(
                id = partnership1Id,
                eventId = eventId,
                companyId = company1Id,
                selectedPackId = packId,
                communicationPublicationDate = null
            )
            
            insertMockedPartnership(
                id = partnership2Id,
                eventId = eventId,
                companyId = company2Id,
                selectedPackId = packId,
                communicationPublicationDate = null
            )
        }

        val response = client.get("/orgs/$orgId/events/$eventSlug/communication") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)

        val responseBody = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        val unplanned = responseBody["unplanned"]!!.jsonArray
        assertEquals(2, unplanned.size)
        
        // Should be sorted by company name ascending (alphabetically)
        assertEquals("Alpha Company", unplanned[0].jsonObject["company_name"]!!.jsonPrimitive.content)
        assertEquals("Zebra Company", unplanned[1].jsonObject["company_name"]!!.jsonPrimitive.content)
    }

    @Test
    fun `GET communication plan returns empty groups when no partnerships exist`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val eventSlug = "test-event-empty"

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)
        }

        val response = client.get("/orgs/$orgId/events/$eventSlug/communication") {
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
        val orgId = UUID.randomUUID()
        val eventSlug = "test-event-unauthorized"

        application {
            moduleMocked()
        }

        val response = client.get("/orgs/$orgId/events/$eventSlug/communication")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `GET communication plan returns 404 for non-existent event`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventSlug = "non-existent-event"

        application {
            moduleMocked()
        }

        val response = client.get("/orgs/$orgId/events/$eventSlug/communication") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertTrue(response.bodyAsText().contains("Event with slug $eventSlug not found"))
    }
}