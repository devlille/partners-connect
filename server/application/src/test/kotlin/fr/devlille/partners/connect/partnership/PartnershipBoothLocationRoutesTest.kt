package fr.devlille.partners.connect.partnership

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.events.factories.insertMockedEventWithOrga
import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.partnership.factories.insertMockedPartnership
import fr.devlille.partners.connect.users.factories.insertMockedAdminUser
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PartnershipBoothLocationRoutesTest {
    @Test
    fun `POST assigns booth location and returns partnership id and location`() = testApplication {
        val eventId = UUID.randomUUID()
        val eventSlug = "test-booth-location-assign"
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleMocked()
            val admin = insertMockedAdminUser()
            val org = insertMockedOrganisationEntity(name = "Test Organization", representativeUser = admin)
            insertMockedEventWithOrga(eventId, organisation = org, slug = eventSlug)
            insertMockedOrgaPermission(orgId = org.id.value, user = admin)
            insertMockedCompany(companyId)
            insertMockedPartnership(id = partnershipId, eventId = eventId, companyId = companyId)
        }

        val response = client.put(
            "/orgs/test-organization/events/$eventSlug/partnership/$partnershipId/booth-location",
        ) {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody("""{"location": "A-12"}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("A-12"))
        assertTrue(body.contains("id"))
    }

    @Test
    fun `POST allows reassigning location to same partnership`() = testApplication {
        val eventId = UUID.randomUUID()
        val eventSlug = "test-booth-location-reassign"
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleMocked()
            val admin = insertMockedAdminUser()
            val org = insertMockedOrganisationEntity(name = "Test Organization", representativeUser = admin)
            insertMockedEventWithOrga(eventId, organisation = org, slug = eventSlug)
            insertMockedOrgaPermission(orgId = org.id.value, user = admin)
            insertMockedCompany(companyId)
            insertMockedPartnership(id = partnershipId, eventId = eventId, companyId = companyId)
        }

        // First assignment
        val response1 = client.put(
            "/orgs/test-organization/events/$eventSlug/partnership/$partnershipId/booth-location",
        ) {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody("""{"location": "B-5"}""")
        }

        assertEquals(HttpStatusCode.OK, response1.status)

        // Reassign to different location
        val response2 = client.put(
            "/orgs/test-organization/events/$eventSlug/partnership/$partnershipId/booth-location",
        ) {
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
        val eventId = UUID.randomUUID()
        val eventSlug = "test-booth-location-conflict"
        val companyId1 = UUID.randomUUID()
        val companyId2 = UUID.randomUUID()
        val partnershipId1 = UUID.randomUUID()
        val partnershipId2 = UUID.randomUUID()

        application {
            moduleMocked()
            val admin = insertMockedAdminUser()
            val org = insertMockedOrganisationEntity(name = "Test Organization", representativeUser = admin)
            insertMockedEventWithOrga(eventId, organisation = org, slug = eventSlug)
            insertMockedOrgaPermission(orgId = org.id.value, user = admin)
            insertMockedCompany(companyId1)
            insertMockedCompany(companyId2)
            insertMockedPartnership(id = partnershipId1, eventId = eventId, companyId = companyId1)
            insertMockedPartnership(id = partnershipId2, eventId = eventId, companyId = companyId2)
        }

        // Assign location to first partnership
        val response1 = client.put(
            "/orgs/test-organization/events/$eventSlug/partnership/$partnershipId1/booth-location",
        ) {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody("""{"location": "D-3"}""")
        }

        assertEquals(HttpStatusCode.OK, response1.status)

        // Try to assign same location to second partnership
        val response2 = client.put(
            "/orgs/test-organization/events/$eventSlug/partnership/$partnershipId2/booth-location",
        ) {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody("""{"location": "D-3"}""")
        }

        assertEquals(HttpStatusCode.Forbidden, response2.status)
        val body = response2.bodyAsText()
        assertTrue(body.contains("already assigned"))
        assertTrue(body.contains("Mock Company"))
    }

    @Test
    fun `POST allows same location across different events`() = testApplication {
        val eventId1 = UUID.randomUUID()
        val eventId2 = UUID.randomUUID()
        val eventSlug1 = "test-booth-location-event1"
        val eventSlug2 = "test-booth-location-event2"
        val companyId1 = UUID.randomUUID()
        val companyId2 = UUID.randomUUID()
        val partnershipId1 = UUID.randomUUID()
        val partnershipId2 = UUID.randomUUID()

        application {
            moduleMocked()
            val admin = insertMockedAdminUser()
            val org = insertMockedOrganisationEntity(name = "Test Organization", representativeUser = admin)
            insertMockedEventWithOrga(eventId1, organisation = org, slug = eventSlug1)
            insertMockedEventWithOrga(eventId2, organisation = org, slug = eventSlug2)
            insertMockedOrgaPermission(orgId = org.id.value, user = admin)
            insertMockedCompany(companyId1)
            insertMockedCompany(companyId2)
            insertMockedPartnership(id = partnershipId1, eventId = eventId1, companyId = companyId1)
            insertMockedPartnership(id = partnershipId2, eventId = eventId2, companyId = companyId2)
        }

        // Assign location to partnership in first event
        val response1 = client.put(
            "/orgs/test-organization/events/$eventSlug1/partnership/$partnershipId1/booth-location",
        ) {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody("""{"location": "E-7"}""")
        }

        assertEquals(HttpStatusCode.OK, response1.status)

        // Assign same location to partnership in second event (should succeed)
        val response2 = client.put(
            "/orgs/test-organization/events/$eventSlug2/partnership/$partnershipId2/booth-location",
        ) {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody("""{"location": "E-7"}""")
        }

        assertEquals(HttpStatusCode.OK, response2.status)
        val body = response2.bodyAsText()
        assertTrue(body.contains("E-7"))
    }

    @Test
    fun `POST returns 400 for empty location`() = testApplication {
        val eventId = UUID.randomUUID()
        val eventSlug = "test-booth-location-empty"
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleMocked()
            val admin = insertMockedAdminUser()
            val org = insertMockedOrganisationEntity(name = "Test Organization", representativeUser = admin)
            insertMockedEventWithOrga(eventId, organisation = org, slug = eventSlug)
            insertMockedOrgaPermission(orgId = org.id.value, user = admin)
            insertMockedCompany(companyId)
            insertMockedPartnership(id = partnershipId, eventId = eventId, companyId = companyId)
        }

        val response = client.put(
            "/orgs/test-organization/events/$eventSlug/partnership/$partnershipId/booth-location",
        ) {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody("""{"location": "   "}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("cannot be empty"))
    }

    @Test
    fun `POST returns 400 for missing location field`() = testApplication {
        val eventId = UUID.randomUUID()
        val eventSlug = "test-booth-location-missing"
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleMocked()
            val admin = insertMockedAdminUser()
            val org = insertMockedOrganisationEntity(name = "Test Organization", representativeUser = admin)
            insertMockedEventWithOrga(eventId, organisation = org, slug = eventSlug)
            insertMockedOrgaPermission(orgId = org.id.value, user = admin)
            insertMockedCompany(companyId)
            insertMockedPartnership(id = partnershipId, eventId = eventId, companyId = companyId)
        }

        val response = client.put(
            "/orgs/test-organization/events/$eventSlug/partnership/$partnershipId/booth-location",
        ) {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody("""{}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `POST returns 401 when Authorization header is missing`() = testApplication {
        val eventId = UUID.randomUUID()
        val eventSlug = "test-booth-location-unauthorized"
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleMocked()
            val admin = insertMockedAdminUser()
            val org = insertMockedOrganisationEntity(name = "Test Organization", representativeUser = admin)
            insertMockedEventWithOrga(eventId, organisation = org, slug = eventSlug)
            insertMockedOrgaPermission(orgId = org.id.value, user = admin)
            insertMockedCompany(companyId)
            insertMockedPartnership(id = partnershipId, eventId = eventId, companyId = companyId)
        }

        val response = client.put(
            "/orgs/test-organization/events/$eventSlug/partnership/$partnershipId/booth-location",
        ) {
            contentType(ContentType.Application.Json)
            setBody("""{"location": "F-9"}""")
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `POST returns 404 for non-existent event`() = testApplication {
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleMocked()
            val admin = insertMockedAdminUser()
            val org = insertMockedOrganisationEntity(name = "Test Organization", representativeUser = admin)
            insertMockedOrgaPermission(orgId = org.id.value, user = admin)
            insertMockedCompany(companyId)
        }

        val response = client.put(
            "/orgs/test-organization/events/non-existent-event/partnership/$partnershipId/booth-location",
        ) {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody("""{"location": "G-1"}""")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `POST returns 404 for non-existent partnership`() = testApplication {
        val eventId = UUID.randomUUID()
        val eventSlug = "test-booth-location-no-partnership"
        val nonExistentPartnershipId = UUID.randomUUID()

        application {
            moduleMocked()
            val admin = insertMockedAdminUser()
            val org = insertMockedOrganisationEntity(name = "Test Organization", representativeUser = admin)
            insertMockedEventWithOrga(eventId, organisation = org, slug = eventSlug)
            insertMockedOrgaPermission(orgId = org.id.value, user = admin)
        }

        val response = client.put(
            "/orgs/test-organization/events/$eventSlug/partnership/$nonExistentPartnershipId/booth-location",
        ) {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody("""{"location": "H-4"}""")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}
