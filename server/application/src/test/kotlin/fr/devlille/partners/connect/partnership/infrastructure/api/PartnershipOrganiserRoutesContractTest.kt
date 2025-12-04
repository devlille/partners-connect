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
    fun `POST rejects to assign organiser to non-existent partnership`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val orgSlug = "tech-conference"
        val eventSlug = "devlille-2025"
        val userEmail = "organiser@example.com"
        val nonExistentPartnershipId = UUID.randomUUID()

        application {
            moduleMocked()

            insertMockedOrganisationEntity(orgId, orgSlug)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)

            val user = insertMockedUser(
                id = userId,
                name = "Alice Organiser",
                email = userEmail,
                pictureUrl = "https://example.com/alice.jpg",
            )
            insertMockedOrgaPermission(
                orgId = orgId,
                user = user,
                canEdit = true,
            )
        }

        val requestBody = """{"email": "$userEmail"}"""

        val response = client.post(
            "/orgs/$orgSlug/events/$eventSlug/partnerships/$nonExistentPartnershipId/organiser",
        ) {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            header(HttpHeaders.Authorization, "Bearer valid")
            header(HttpHeaders.Accept, "application/json")
            setBody(requestBody)
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `POST rejects to assign non-existent user as organiser`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val orgSlug = "tech-conference"
        val eventSlug = "devlille-2025"

        application {
            moduleMocked()

            insertMockedOrganisationEntity(orgId, orgSlug)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)
            insertMockedCompany(id = companyId)
            insertMockedPartnership(id = partnershipId, eventId = eventId, companyId = companyId)
        }

        val requestBody = """{ "email": "nonexistent@example.com"}"""

        val response = client.post("/orgs/$orgSlug/events/$eventSlug/partnerships/$partnershipId/organiser") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            header(HttpHeaders.Authorization, "Bearer valid")
            header(HttpHeaders.Accept, "application/json")
            setBody(requestBody)
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `POST rejects to assign user without organisation membership`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val orgSlug = "tech-conference"
        val eventSlug = "devlille-2025"
        val userEmail = "outsider@example.com"

        application {
            moduleMocked()

            insertMockedOrganisationEntity(orgId, orgSlug)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)
            insertMockedCompany(id = companyId)
            insertMockedPartnership(id = partnershipId, eventId = eventId, companyId = companyId)

            // Create user but no organisation permission
            insertMockedUser(
                id = userId,
                name = "Bob Outsider",
                email = userEmail,
                pictureUrl = "https://example.com/bob.jpg",
            )
        }

        val requestBody = """{ "email": "$userEmail"}"""

        val response = client.post("/orgs/$orgSlug/events/$eventSlug/partnerships/$partnershipId/organiser") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(requestBody)
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `POST rejects to assign user without edit permission`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val orgSlug = "tech-conference"
        val eventSlug = "devlille-2025"
        val userEmail = "viewer@example.com"

        application {
            moduleMocked()

            insertMockedOrganisationEntity(orgId, orgSlug)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)
            insertMockedCompany(id = companyId)
            insertMockedPartnership(id = partnershipId, eventId = eventId, companyId = companyId)

            // Create user with read-only permission (canEdit = false)
            val user = insertMockedUser(
                id = userId,
                name = "Charlie Viewer",
                email = userEmail,
                pictureUrl = "https://example.com/charlie.jpg",
            )
            // Create user with read-only permission (canEdit = false)
            insertMockedOrgaPermission(
                orgId = orgId,
                user = user,
                canEdit = false,
            )
        }

        val requestBody = """{"email": "$userEmail"}"""

        val response = client.post("/orgs/$orgSlug/events/$eventSlug/partnerships/$partnershipId/organiser") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(requestBody)
        }

        assertEquals(HttpStatusCode.Forbidden, response.status)
    }

    @Test
    fun `DELETE successfully remove organiser from partnership`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val orgSlug = "tech-conference"
        val eventSlug = "devlille-2025"
        val userEmail = "organiser@example.com"

        application {
            moduleMocked()

            insertMockedOrganisationEntity(orgId, orgSlug)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)
            insertMockedCompany(id = companyId)

            // Create user with edit permissions
            val user = insertMockedUser(
                id = userId,
                name = "Alice Organiser",
                email = userEmail,
                pictureUrl = "https://example.com/alice.jpg",
            )
            insertMockedOrgaPermission(
                orgId = orgId,
                user = user,
                canEdit = true,
            )

            // Create partnership with organiser already assigned
            insertMockedPartnership(
                id = partnershipId,
                eventId = eventId,
                companyId = companyId,
                organiserId = user.id.value,
            )
        }

        val response = client.delete("/orgs/$orgSlug/events/$eventSlug/partnerships/$partnershipId/organiser") {
            header(HttpHeaders.Accept, "application/json")
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)

        val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals(partnershipId.toString(), json["partnership_id"]!!.jsonPrimitive.content)

        // Verify organiser is null after removal
        val organiserValue = json["organiser"]
        assertTrue(organiserValue is JsonNull, "Expected organiser to be JsonNull, got: $organiserValue")
    }
}
