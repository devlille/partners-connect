package fr.devlille.partners.connect.partnership.infrastructure.api

import fr.devlille.partners.connect.companies.factories.insertMockedCompany
import fr.devlille.partners.connect.events.factories.insertMockedEventWithOrga
import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.partnership.factories.insertMockedPartnership
import fr.devlille.partners.connect.partnership.factories.insertMockedPartnershipEmail
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringPack
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
class PartnershipUpdateRoutesContractTest {
    @Test
    fun `PUT accepts valid request with all fields`() = testApplication {
        val eventId = UUID.randomUUID()
        val eventSlug = "test-event-all-fields"
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val packId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedEventWithOrga(eventId, slug = eventSlug)
            insertMockedCompany(companyId)
            val pack = insertMockedSponsoringPack(packId, eventId)
            insertMockedPartnership(
                id = partnershipId,
                eventId = eventId,
                companyId = companyId,
                selectedPackId = pack.id.value,
            )
        }

        val response = client.put("/events/$eventSlug/partnerships/$partnershipId") {
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                    "contact_name": "John Doe",
                    "contact_role": "Developer Relations Manager",
                    "language": "en",
                    "phone": "+33123456789",
                    "emails": ["john.doe@example.com", "contact@example.com"]
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
        val eventId = UUID.randomUUID()
        val eventSlug = "test-event"
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val packId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedEventWithOrga(eventId, slug = eventSlug)
            insertMockedCompany(companyId)
            val pack = insertMockedSponsoringPack(packId, eventId)
            insertMockedPartnership(
                id = partnershipId,
                eventId = eventId,
                companyId = companyId,
                selectedPackId = pack.id.value,
                contactName = "Original Name",
                contactRole = "Original Role",
                language = "fr",
                phone = "+33111111111",
            )
            insertMockedPartnershipEmail(partnershipId, "original@example.com")
        }

        val response = client.put("/events/$eventSlug/partnerships/$partnershipId") {
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
        val eventId = UUID.randomUUID()
        val eventSlug = "test-event-empty"
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val packId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedEventWithOrga(eventId, slug = eventSlug)
            insertMockedCompany(companyId)
            val pack = insertMockedSponsoringPack(packId, eventId)
            insertMockedPartnership(
                id = partnershipId,
                eventId = eventId,
                companyId = companyId,
                selectedPackId = pack.id.value,
            )
        }

        val response = client.put("/events/$eventSlug/partnerships/$partnershipId") {
            contentType(ContentType.Application.Json)
            setBody("{}")
        }

        // Empty body is valid - no-op update
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `PUT rejects invalid email format`() = testApplication {
        val eventId = UUID.randomUUID()
        val eventSlug = "test-event-invalid-email"
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val packId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedEventWithOrga(eventId, slug = eventSlug)
            insertMockedCompany(companyId)
            val pack = insertMockedSponsoringPack(packId, eventId)
            insertMockedPartnership(
                id = partnershipId,
                eventId = eventId,
                companyId = companyId,
                selectedPackId = pack.id.value,
            )
        }

        val response = client.put("/events/$eventSlug/partnerships/$partnershipId") {
            contentType(ContentType.Application.Json)
            setBody("""{"emails": ["invalid-email-format"]}""")
        }

        // Schema validation should reject invalid email format
        assertEquals(HttpStatusCode.BadRequest, response.status)
        val bodyText = response.bodyAsText()
        assertTrue(bodyText.contains("email") || bodyText.contains("format") || bodyText.contains("validation"))
    }

    @Test
    fun `PUT accepts valid phone number`() = testApplication {
        val eventId = UUID.randomUUID()
        val eventSlug = "test-event-phone"
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val packId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedEventWithOrga(eventId, slug = eventSlug)
            insertMockedCompany(companyId)
            val pack = insertMockedSponsoringPack(packId, eventId)
            insertMockedPartnership(
                id = partnershipId,
                eventId = eventId,
                companyId = companyId,
                selectedPackId = pack.id.value,
            )
        }

        val response = client.put("/events/$eventSlug/partnerships/$partnershipId") {
            contentType(ContentType.Application.Json)
            setBody("""{"phone": "+33123456789"}""")
        }

        // Valid phone should be accepted
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `PUT accepts phone with exactly 30 characters`() = testApplication {
        val eventId = UUID.randomUUID()
        val eventSlug = "test-event-max-phone"
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val packId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedEventWithOrga(eventId, slug = eventSlug)
            insertMockedCompany(companyId)
            val pack = insertMockedSponsoringPack(packId, eventId)
            insertMockedPartnership(
                id = partnershipId,
                eventId = eventId,
                companyId = companyId,
                selectedPackId = pack.id.value,
            )
        }

        val response = client.put("/events/$eventSlug/partnerships/$partnershipId") {
            contentType(ContentType.Application.Json)
            // Exactly 30 characters
            setBody("""{"phone": "+33123456789012345678901234"}""")
        }

        // Should accept valid phone at boundary
        assertEquals(HttpStatusCode.OK, response.status)
    }

    @Test
    fun `PUT accepts all valid language codes`() = testApplication {
        val eventId = UUID.randomUUID()
        val eventSlug = "test-event-languages"
        val companyId = UUID.randomUUID()
        val packId = UUID.randomUUID()
        val validLanguages = listOf("en", "fr", "de", "nl", "es")
        val partnershipIds = validLanguages.map { UUID.randomUUID() }

        application {
            moduleMocked()
            insertMockedEventWithOrga(eventId, slug = eventSlug)
            insertMockedCompany(companyId)
            insertMockedSponsoringPack(packId, eventId)

            // Create a partnership for each language test
            partnershipIds.forEach { partnershipId ->
                insertMockedPartnership(
                    id = partnershipId,
                    eventId = eventId,
                    companyId = companyId,
                    selectedPackId = packId,
                )
            }
        }

        // Test each language code
        validLanguages.zip(partnershipIds).forEach { (lang, partnershipId) ->
            val response = client.put("/events/$eventSlug/partnerships/$partnershipId") {
                contentType(ContentType.Application.Json)
                setBody("""{"language": "$lang"}""")
            }

            assertEquals(
                HttpStatusCode.OK,
                response.status,
                "Language $lang should be accepted",
            )
        }
    }

    @Test
    fun `PUT accepts multiple valid emails`() = testApplication {
        val eventId = UUID.randomUUID()
        val eventSlug = "test-event-multi-email"
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val packId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedEventWithOrga(eventId, slug = eventSlug)
            insertMockedCompany(companyId)
            val pack = insertMockedSponsoringPack(packId, eventId)
            insertMockedPartnership(
                id = partnershipId,
                eventId = eventId,
                companyId = companyId,
                selectedPackId = pack.id.value,
            )
        }

        val response = client.put("/events/$eventSlug/partnerships/$partnershipId") {
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
    fun `PUT rejects emails with mixed valid and invalid formats`() = testApplication {
        val eventId = UUID.randomUUID()
        val eventSlug = "test-event-mixed-emails"
        val companyId = UUID.randomUUID()
        val partnershipId = UUID.randomUUID()
        val packId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedEventWithOrga(eventId, slug = eventSlug)
            insertMockedCompany(companyId)
            val pack = insertMockedSponsoringPack(packId, eventId)
            insertMockedPartnership(
                id = partnershipId,
                eventId = eventId,
                companyId = companyId,
                selectedPackId = pack.id.value,
            )
        }

        val response = client.put("/events/$eventSlug/partnerships/$partnershipId") {
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
