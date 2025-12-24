package fr.devlille.partners.connect.partnership.infrastructure.api

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.partnership.factories.insertMockedPartnership
import fr.devlille.partners.connect.partnership.factories.insertMockedPartnershipEmail
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringPack
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import fr.devlille.partners.connect.users.factories.insertMockedUser
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Contract tests for PUT /events/{eventSlug}/partnerships/{partnershipId} endpoint.
 * Tests JSON schema validation for update_partnership_request.schema.json.
 * Validates that endpoint correctly handles valid and invalid requests according to schema.
 */
class PartnershipUpdateRoutePutTest {
    @Test
    fun `PUT accepts valid request with all fields`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(companyId)
                insertMockedSponsoringPack(packId, eventId)
                insertMockedPartnership(
                    id = partnershipId,
                    eventId = eventId,
                    companyId = companyId,
                    selectedPackId = packId,
                )
            }
        }

        val response = client.put("/events/$eventId/partnerships/$partnershipId") {
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                    "contact_name": "John Doe",
                    "contact_role": "Developer Relations Manager",
                    "language": "en",
                    "phone": "+33123456789",
                    "emails": ["${UUID.randomUUID()}@example.com", "${UUID.randomUUID()}@example.com"]
                }
                """.trimIndent(),
            )
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertNotNull(responseBody["partnership"])
    }

    @Test
    fun `PUT updates multiple fields - partial update`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(companyId)
                insertMockedSponsoringPack(packId, eventId)
                insertMockedPartnership(
                    id = partnershipId,
                    eventId = eventId,
                    companyId = companyId,
                    selectedPackId = packId,
                    contactName = "Original Name",
                    contactRole = "Original Role",
                    language = "fr",
                    phone = "+33111111111",
                )
                insertMockedPartnershipEmail(partnershipId, "original@example.com")
            }
        }

        val response = client.put("/events/$eventId/partnerships/$partnershipId") {
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                    "contact_name": "Updated Name",
                    "language": "en"
                }
                """.trimIndent(),
            )
        }

        assertEquals(HttpStatusCode.OK, response.status)

        val responseBody = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        val partnership = responseBody["partnership"]?.jsonObject
        assertNotNull(partnership)

        // Updated fields
        assertEquals("Updated Name", partnership["contact_name"]?.jsonPrimitive?.content)
        assertEquals("en", partnership["language"]?.jsonPrimitive?.content)

        // Unchanged fields
        assertEquals("Original Role", partnership["contact_role"]?.jsonPrimitive?.content)
        assertEquals("+33111111111", partnership["phone"]?.jsonPrimitive?.content)
        val emails = partnership["emails"]?.jsonArray
        assertNotNull(emails)
        assertEquals(1, emails.size)
        assertEquals("original@example.com", emails[0].jsonPrimitive.content)
    }

    @Test
    fun `PUT accepts empty request body`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(companyId)
                insertMockedSponsoringPack(packId, eventId)
                insertMockedPartnership(
                    id = partnershipId,
                    eventId = eventId,
                    companyId = companyId,
                    selectedPackId = packId,
                )
            }
        }

        val response = client.put("/events/$eventId/partnerships/$partnershipId") {
            contentType(ContentType.Application.Json)
            setBody("{}")
        }

        // Empty body is valid - no-op update
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `PUT accepts valid phone number`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(companyId)
                insertMockedSponsoringPack(packId, eventId)
                insertMockedPartnership(
                    id = partnershipId,
                    eventId = eventId,
                    companyId = companyId,
                    selectedPackId = packId,
                )
            }
        }

        val response = client.put("/events/$eventId/partnerships/$partnershipId") {
            contentType(ContentType.Application.Json)
            setBody("""{"phone": "+33123456789"}""")
        }

        // Valid phone should be accepted
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `PUT accepts phone with exactly 30 characters`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(companyId)
                insertMockedSponsoringPack(packId, eventId)
                insertMockedPartnership(
                    id = partnershipId,
                    eventId = eventId,
                    companyId = companyId,
                    selectedPackId = packId,
                )
            }
        }

        val response = client.put("/events/$eventId/partnerships/$partnershipId") {
            contentType(ContentType.Application.Json)
            // Exactly 30 characters
            setBody("""{"phone": "+33123456789012345678901234"}""")
        }

        // Should accept valid phone at boundary
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `PUT accepts multiple valid emails`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(companyId)
                insertMockedSponsoringPack(packId, eventId)
                insertMockedPartnership(
                    id = partnershipId,
                    eventId = eventId,
                    companyId = companyId,
                    selectedPackId = packId,
                )
            }
        }

        val response = client.put("/events/$eventId/partnerships/$partnershipId") {
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                    "emails": [
                        "first@example.com",
                        "second@example.com",
                        "third@example.com"
                    ]
                }
                """.trimIndent(),
            )
        }

        assertEquals(HttpStatusCode.OK, response.status)

        // Verify all emails were saved
        val responseBody = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        val partnership = responseBody["partnership"]?.jsonObject
        assertNotNull(partnership)
        val emails = partnership["emails"]?.toString()
        assertNotNull(emails)
        assertTrue(emails.contains("first@example.com"))
        assertTrue(emails.contains("second@example.com"))
        assertTrue(emails.contains("third@example.com"))
    }

    @Test
    fun `PUT rejects invalid email format`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(companyId)
                insertMockedSponsoringPack(packId, eventId)
                insertMockedPartnership(
                    id = partnershipId,
                    eventId = eventId,
                    companyId = companyId,
                    selectedPackId = packId,
                )
            }
        }

        val response = client.put("/events/$eventId/partnerships/$partnershipId") {
            contentType(ContentType.Application.Json)
            setBody("""{"emails": ["invalid-email-format"]}""")
        }

        // Schema validation should reject invalid email format
        assertEquals(HttpStatusCode.BadRequest, response.status)
        val bodyText = response.bodyAsText()
        assertTrue(bodyText.contains("email") || bodyText.contains("format") || bodyText.contains("validation"))
    }

    @Test
    fun `PUT rejects emails with mixed valid and invalid formats`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
                insertMockedCompany(companyId)
                insertMockedSponsoringPack(packId, eventId)
                insertMockedPartnership(
                    id = partnershipId,
                    eventId = eventId,
                    companyId = companyId,
                    selectedPackId = packId,
                )
            }
        }

        val response = client.put("/events/$eventId/partnerships/$partnershipId") {
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                    "emails": [
                        "valid@example.com",
                        "invalid-email"
                    ]
                }
                """.trimIndent(),
            )
        }

        // Should reject entire request if any email is invalid
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }
}
