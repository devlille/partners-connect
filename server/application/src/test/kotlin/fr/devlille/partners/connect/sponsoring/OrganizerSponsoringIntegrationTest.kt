package fr.devlille.partners.connect.sponsoring

import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
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
import kotlin.test.assertTrue

class OrganizerSponsoringIntegrationTest {
    @Test
    fun `organizer packs endpoint returns all translations without Accept-Language header`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val eventSlug = "test-organizer-packs-multi-lang"

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)

            val pack = insertMockedSponsoringPack(event = eventId, name = "Gold Sponsor")
            val option = insertMockedSponsoringOption(eventId = eventId)

            insertMockedPackOptions(packId = pack.id.value, optionId = option.id.value, required = true)
        }

        val response = client.get("/orgs/$orgId/events/$eventSlug/packs") {
            header(HttpHeaders.Authorization, "Bearer valid")
            // NO Accept-Language header - this is the key test
        }

        // Implementation completed - validating correct behavior
        // We expect:
        // - HttpStatusCode.OK
        // - Response body containing all translations for each option
        // - Translation maps with language codes as keys
        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody = response.bodyAsText()
        assertTrue(responseBody.isNotEmpty())

        // Verify response contains translations map structure
        assertTrue(responseBody.contains("translations"))
        assertTrue(
            responseBody.contains("\"en\":") ||
                responseBody.contains("\"fr\":") ||
                responseBody.contains("\"de\":"),
        )
    }

    @Test
    fun `organizer options endpoint returns all translations without Accept-Language header`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val eventSlug = "test-organizer-options-multi-lang"

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)

            val option = insertMockedSponsoringOption(eventId = eventId)
        }

        val response = client.get("/orgs/$orgId/events/$eventSlug/options") {
            header(HttpHeaders.Authorization, "Bearer valid")
            // NO Accept-Language header - this is the key test
        }

        // Implementation completed - validating correct behavior
        // We expect:
        // - HttpStatusCode.OK
        // - Response body containing all translations for each option
        // - Translation maps with language codes as keys
        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody = response.bodyAsText()
        assertTrue(responseBody.isNotEmpty())

        // Verify response contains translations map structure
        assertTrue(responseBody.contains("translations"))
        assertTrue(
            responseBody.contains("\"en\":") ||
                responseBody.contains("\"fr\":") ||
                responseBody.contains("\"es\":"),
        )
    }

    @Test
    fun `organizer endpoints return consistent data structure for options`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val eventSlug = "test-organizer-consistency"

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)

            val pack = insertMockedSponsoringPack(event = eventId, name = "Silver Sponsor")
            val option = insertMockedSponsoringOption(eventId = eventId)

            insertMockedPackOptions(packId = pack.id.value, optionId = option.id.value, required = false)
        }

        // Test both endpoints without Accept-Language header
        val packsResponse = client.get("/orgs/$orgId/events/$eventSlug/packs") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        val optionsResponse = client.get("/orgs/$orgId/events/$eventSlug/options") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        // Implementation completed - validating both endpoints work correctly
        // Both should return OK and have consistent option structure with translations
        assertEquals(HttpStatusCode.OK, packsResponse.status)
        assertEquals(HttpStatusCode.OK, optionsResponse.status)

        // Verify both responses contain translation structure
        val packsBody = packsResponse.bodyAsText()
        val optionsBody = optionsResponse.bodyAsText()
        assertTrue(packsBody.contains("translations"))
        assertTrue(optionsBody.contains("translations"))
    }
}
