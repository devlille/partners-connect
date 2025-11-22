package fr.devlille.partners.connect.partnership

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.partnership.factories.insertMockedPartnership
import fr.devlille.partners.connect.users.factories.insertMockedEventWithAdminUser
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import fr.devlille.partners.connect.users.factories.insertMockedUser
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Integration tests for organiser assignment and removal on partnerships.
 * Tests the complete HTTP request/response cycle including authorization and validation.
 */
class PartnershipOrganiserIntegrationTest {
    @Test
    fun `successfully assign organiser to partnership`() = testApplication {
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
            insertMockedPartnership(id = partnershipId, eventId = eventId, companyId = companyId)

            // Create user with edit permissions for the organisation
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

        val response = client.post("/orgs/$orgSlug/events/$eventSlug/partnerships/$partnershipId/organiser") {
            header(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(requestBody)
        }

        assertEquals(HttpStatusCode.OK, response.status)

        val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertEquals(partnershipId.toString(), json["partnership_id"]!!.jsonPrimitive.content)

        val organiser = json["organiser"]!!.jsonObject
        assertEquals("Alice Organiser", organiser["display_name"]!!.jsonPrimitive.content)
        assertEquals(userEmail, organiser["email"]!!.jsonPrimitive.content)
        assertEquals("https://example.com/alice.jpg", organiser["picture_url"]!!.jsonPrimitive.content)
    }

    @Test
    fun `fail to assign non-existent user as organiser`() = testApplication {
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
    fun `fail to assign user without organisation membership`() = testApplication {
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
    fun `fail to assign user without edit permission`() = testApplication {
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
    fun `successfully remove organiser from partnership`() = testApplication {
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

    @Test
    fun `fail to assign organiser to non-existent partnership`() = testApplication {
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
    fun `organiser field is included in partnership detail response`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val userId = UUID.randomUUID()
        val eventSlug = "devlille-2025"
        val userEmail = "organiser@example.com"

        application {
            moduleMocked()

            @Suppress("unused")
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)
            insertMockedCompany(id = companyId)

            val user = insertMockedUser(
                id = userId,
                name = "Alice Organiser",
                email = userEmail,
                pictureUrl = "https://example.com/alice.jpg",
            )

            insertMockedPartnership(
                id = partnershipId,
                eventId = eventId,
                companyId = companyId,
                organiserId = user.id.value,
            )
        }

        val response = client.get("/events/$eventSlug/partnerships/$partnershipId") {
            header(HttpHeaders.Accept, "application/json")
        }

        assertEquals(HttpStatusCode.OK, response.status)

        val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertNotNull(json["partnership"])

        val partnership = json["partnership"]!!.jsonObject
        assertNotNull(partnership["organiser"])

        val organiser = partnership["organiser"]!!.jsonObject
        assertEquals("Alice Organiser", organiser["display_name"]!!.jsonPrimitive.content)
        assertEquals(userEmail, organiser["email"]!!.jsonPrimitive.content)
        assertEquals("https://example.com/alice.jpg", organiser["picture_url"]!!.jsonPrimitive.content)
    }

    @Test
    fun `organiser field is null when no organiser assigned`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val eventSlug = "devlille-2025"

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)
            insertMockedCompany(id = companyId)

            insertMockedPartnership(
                id = partnershipId,
                eventId = eventId,
                companyId = companyId,
                // No organiser assigned
            )
        }

        val response = client.get("/events/$eventSlug/partnerships/$partnershipId") {
            header(HttpHeaders.Accept, "application/json")
        }

        assertEquals(HttpStatusCode.OK, response.status)

        val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        val partnership = json["partnership"]!!.jsonObject

        // Organiser field should exist but be null
        val organiserValue = partnership["organiser"]
        assertNull(organiserValue?.jsonPrimitive?.content)
    }
}
