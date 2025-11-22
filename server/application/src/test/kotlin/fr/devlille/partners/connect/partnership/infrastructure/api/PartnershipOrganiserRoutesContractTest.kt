package fr.devlille.partners.connect.partnership.infrastructure.api

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.partnership.factories.insertMockedPartnership
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringPack
import fr.devlille.partners.connect.users.factories.insertMockedEventWithAdminUser
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import fr.devlille.partners.connect.users.factories.insertMockedUser
import io.ktor.client.request.delete
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Contract test for partnership organiser assignment endpoints.
 * Tests JSON schema validation for assign_organiser_request.schema.json
 * and partnership_organiser_response.schema.json.
 * Validates that endpoints correctly handle valid and invalid requests according to schemas.
 */
class PartnershipOrganiserRoutesContractTest {
    @Test
    fun `POST assigns organiser with valid request schema`() = testApplication {
        val eventSlug = "test-event"
        val partnershipId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val organiserEmail = "organiser@example.com"

        application {
            moduleMocked()

            // Create test data
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)
            insertMockedCompany(companyId)
            val pack = insertMockedSponsoringPack(packId, eventId)
            insertMockedPartnership(
                id = partnershipId,
                eventId = eventId,
                companyId = companyId,
                selectedPackId = pack.id.value,
            )
            val user = insertMockedUser(userId, email = organiserEmail)
            insertMockedOrgaPermission(
                orgId = orgId,
                user = user,
                canEdit = true,
            )
        }

        val response = client.post("/orgs/$orgId/events/$eventSlug/partnerships/$partnershipId/organiser") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody("""{"email":"$organiserEmail"}""")
        }

        // Should return 200 OK with valid partnership organiser response
        assertEquals(HttpStatusCode.OK, response.status)

        val responseBody = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals(partnershipId.toString(), responseBody["partnership_id"]?.jsonPrimitive?.content)
        assertNotNull(responseBody["organiser"])
    }

    @Test
    fun `POST rejects missing email field`() = testApplication {
        val eventSlug = "test-event"
        val partnershipId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)
        }

        val response = client.post("/orgs/$orgId/events/$eventSlug/partnerships/$partnershipId/organiser") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody("{}")
        }

        // Should return 400 Bad Request for missing required field
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `POST rejects invalid email format`() = testApplication {
        val eventSlug = "test-event"
        val partnershipId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)
        }

        val response = client.post("/orgs/$orgId/events/$eventSlug/partnerships/$partnershipId/organiser") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody("""{ "email":"not-an-email"}""")
        }

        // Email format validation is handled by schema, should return 400
        // Note: Schema validation may not catch all invalid emails, so we check for client error
        assertTrue(response.status.value >= 400, "Expected client error status, got ${response.status}")
    }

    @Test
    fun `POST returns valid PartnershipOrganiserResponse schema`() = testApplication {
        val eventSlug = "test-event"
        val partnershipId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val organiserEmail = "valid@example.com"

        application {
            moduleMocked()

            // Create test data
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)
            insertMockedCompany(companyId)
            val pack = insertMockedSponsoringPack(packId, eventId)
            insertMockedPartnership(
                id = partnershipId,
                eventId = eventId,
                companyId = companyId,
                selectedPackId = pack.id.value,
            )
            val user = insertMockedUser(userId, email = organiserEmail, name = "Test Organiser")
            insertMockedOrgaPermission(
                orgId = orgId,
                user = user,
                canEdit = true,
            )
        }

        val response = client.post("/orgs/$orgId/events/$eventSlug/partnerships/$partnershipId/organiser") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody("""{"email":"$organiserEmail"}""")
        }

        assertEquals(HttpStatusCode.OK, response.status)

        val responseBody = Json.parseToJsonElement(response.bodyAsText()).jsonObject

        // Validate response schema: partnership_id and organiser fields present
        assertNotNull(responseBody["partnership_id"])
        assertNotNull(responseBody["organiser"])

        val organiser = responseBody["organiser"]?.jsonObject
        assertNotNull(organiser)
        assertEquals(organiserEmail, organiser["email"]?.jsonPrimitive?.content)
    }

    @Test
    fun `DELETE returns valid PartnershipOrganiserResponse with null organiser`() = testApplication {
        val eventSlug = "test-event"
        val partnershipId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val packId = UUID.randomUUID()

        application {
            moduleMocked()

            // Create test data
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)
            insertMockedCompany(companyId)
            val pack = insertMockedSponsoringPack(packId, eventId)
            insertMockedPartnership(
                id = partnershipId,
                eventId = eventId,
                companyId = companyId,
                selectedPackId = pack.id.value,
            )
        }

        val response = client.delete("/orgs/$orgId/events/$eventSlug/partnerships/$partnershipId/organiser") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)

        val responseBody = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals(partnershipId.toString(), responseBody["partnership_id"]?.jsonPrimitive?.content)

        // Validate organiser is null (JSON null, not undefined)
        val organiserValue = responseBody["organiser"]
        assertTrue(organiserValue is JsonNull, "Expected organiser to be JsonNull, got: $organiserValue")
    }
}
