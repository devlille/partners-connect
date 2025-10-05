package fr.devlille.partners.connect.sponsoring

import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.sponsoring.factories.insertMockedOptionTranslation
import fr.devlille.partners.connect.sponsoring.factories.insertMockedPackOptions
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringOption
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringPack
import fr.devlille.partners.connect.users.factories.insertMockedEventWithAdminUser
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PublicEndpointCompatibilityTest {
    @Test
    fun `public endpoints still require Accept-Language header after organizer changes`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val eventSlug = "test-public-compatibility"

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)

            val pack = insertMockedSponsoringPack(event = eventId, name = "Public Pack")
            val option = insertMockedSponsoringOption(eventId = eventId)

            insertMockedOptionTranslation(
                optionId = option.id.value,
                language = "en",
                name = "Public option",
                description = "Public option description",
            )
            insertMockedPackOptions(packId = pack.id.value, optionId = option.id.value, required = true)
        }

        // Test public endpoint WITHOUT Accept-Language header
        val responseWithoutHeader = client.get("/events/$eventSlug/sponsoring/packs") {
            // Intentionally omit Accept-Language header
        }

        // Should still return 400 Bad Request - no change to public API
        assertEquals(HttpStatusCode.BadRequest, responseWithoutHeader.status)
        assertTrue(
            responseWithoutHeader.bodyAsText().contains("accept-language") ||
                responseWithoutHeader.bodyAsText().contains("Accept-Language"),
        )
    }

    @Test
    fun `public endpoints still work WITH Accept-Language header after organizer changes`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val eventSlug = "test-public-works"

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)

            val pack = insertMockedSponsoringPack(event = eventId, name = "Public Pack")
            val option = insertMockedSponsoringOption(eventId = eventId)

            insertMockedOptionTranslation(
                optionId = option.id.value,
                language = "en",
                name = "Public option",
                description = "Public option description",
            )
            insertMockedPackOptions(packId = pack.id.value, optionId = option.id.value, required = true)
        }

        // Test public endpoint WITH Accept-Language header
        val responseWithHeader = client.get("/events/$eventSlug/sponsoring/packs") {
            header(HttpHeaders.AcceptLanguage, "en")
        }

        // Should still work as before - returns 200 OK with single-language response
        assertEquals(HttpStatusCode.OK, responseWithHeader.status)

        // Verify response format is still single-language (not multi-language)
        val responseBody = responseWithHeader.bodyAsText()
        assertTrue(responseBody.contains("\"name\"")) // Should have single name field
        assertTrue(!responseBody.contains("\"translations\"")) // Should NOT have translations map
    }

    @Test
    fun `verify organizer and public endpoints have different response formats`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val eventSlug = "test-format-difference"

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)

            val pack = insertMockedSponsoringPack(event = eventId, name = "Test Pack")
            val option = insertMockedSponsoringOption(eventId = eventId)

            insertMockedOptionTranslation(
                optionId = option.id.value,
                language = "en",
                name = "Test option",
                description = "Test description",
            )
            insertMockedPackOptions(packId = pack.id.value, optionId = option.id.value, required = true)
        }

        // Test public endpoint (should work)
        val publicResponse = client.get("/events/$eventSlug/sponsoring/packs") {
            header(HttpHeaders.AcceptLanguage, "en")
        }
        assertEquals(HttpStatusCode.OK, publicResponse.status)

        // Test organizer endpoint (should fail initially, then work after implementation)
        val organizerResponse = client.get("/orgs/$orgId/events/$eventSlug/packs") {
            header(HttpHeaders.Authorization, "Bearer valid")
            // NO Accept-Language header
        }

        // Implementation completed - organizer endpoint now works and has different response format
        assertEquals(HttpStatusCode.OK, organizerResponse.status)

        // Verify different response formats:
        val publicBody = publicResponse.bodyAsText()
        val organizerBody = organizerResponse.bodyAsText()

        // Public response: options have single name/description fields
        assertTrue(publicBody.contains("\"name\":"))
        assertFalse(publicBody.contains("translations"))

        // Organizer response: options have translations map
        assertTrue(organizerBody.contains("translations"))
        assertTrue(organizerBody.contains("\"en\":") || organizerBody.contains("\"fr\":"))
    }
}
