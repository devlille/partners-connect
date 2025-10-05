package fr.devlille.partners.connect.sponsoring

import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.sponsoring.domain.CreateSponsoringOption
import fr.devlille.partners.connect.sponsoring.domain.SponsoringOptionWithTranslations
import fr.devlille.partners.connect.sponsoring.domain.TranslatedLabel
import fr.devlille.partners.connect.users.factories.insertMockedEventWithAdminUser
import io.ktor.client.call.body
import io.ktor.client.request.get
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
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SponsoringOptionRoutesTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `GET returns empty list when no options exist`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val eventSlug = "test-event-slug-1"
        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)
        }

        val response = client.get("/orgs/$orgId/events/$eventSlug/options") {
            header(HttpHeaders.AcceptLanguage, "fr")
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("[]", response.bodyAsText())
    }

    @Test
    fun `POST creates an option with translations and GET returns it`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val eventSlug = "test-event-slug-2"
        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)
        }

        val request = CreateSponsoringOption(
            translations = listOf(
                TranslatedLabel(language = "fr", name = "Option FR", description = "Description FR"),
                TranslatedLabel(language = "en", name = "Option EN", description = "Description EN"),
            ),
            price = 100,
        )

        val postResponse = client.post("/orgs/$orgId/events/$eventSlug/options") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(request))
        }

        assertEquals(HttpStatusCode.Created, postResponse.status)

        val createResult = json.decodeFromString<Map<String, String>>(postResponse.bodyAsText())
        assertNotNull(createResult["id"])

        val getResponse = client.get("/orgs/$orgId/events/$eventSlug/options") {
            header(HttpHeaders.Authorization, "Bearer valid")
            // No Accept-Language header needed - organizer endpoint returns all translations
        }

        assertEquals(HttpStatusCode.OK, getResponse.status)
        val body = json.decodeFromString<List<SponsoringOptionWithTranslations>>(getResponse.body())
        assertEquals(1, body.size)

        val option = body.first()
        // Verify both translations are present
        assertTrue(option.translations.containsKey("fr"))
        assertTrue(option.translations.containsKey("en"))
        assertEquals("Option FR", option.translations["fr"]?.name)
        assertEquals("Option EN", option.translations["en"]?.name)
        assertEquals("Description FR", option.translations["fr"]?.description)
        assertEquals("Description EN", option.translations["en"]?.description)
    }

    @Test
    fun `GET succeeds without Accept-Language header for organizer endpoints`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val eventSlug = "test-event-slug-3"
        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)
        }

        val response = client.get("/orgs/$orgId/events/$eventSlug/options") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }
        // Organizer endpoints now work without Accept-Language header
        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody = response.bodyAsText()
        assertTrue(responseBody.isNotEmpty())
    }

    @Test
    fun `POST creates option and GET returns all translations regardless of Accept-Language`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val eventSlug = "test-event-slug-4"
        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)
        }

        val request = CreateSponsoringOption(
            translations = listOf(
                TranslatedLabel(language = "fr", name = "Nom FR"),
            ),
        )

        client.post("/orgs/$orgId/events/$eventSlug/options") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(request))
        }

        val response = client.get("/orgs/$orgId/events/$eventSlug/options") {
            header(HttpHeaders.AcceptLanguage, "de") // Unsupported language, but should still work
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        // Organizer endpoints return all translations, so they don't fail on missing languages
        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody = response.bodyAsText()
        assertTrue(responseBody.contains("translations"))
        assertTrue(responseBody.contains("\"fr\":")) // Should contain the French translation
    }

    @Test
    fun `GET returns all options with all translations without Accept-Language header`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val eventSlug = "test-multi-language-options"
        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)
        }

        val request = CreateSponsoringOption(
            translations = listOf(
                TranslatedLabel(
                    language = "en",
                    name = "Logo on website",
                    description = "Company logo displayed on event website",
                ),
                TranslatedLabel(
                    language = "fr",
                    name = "Logo sur le site web",
                    description = "Logo de l'entreprise affiché sur le site de l'événement",
                ),
                TranslatedLabel(language = "de", name = "Logo auf Website", description = ""),
            ),
            price = null,
        )

        client.post("/orgs/$orgId/events/$eventSlug/options") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(request))
        }

        val response = client.get("/orgs/$orgId/events/$eventSlug/options") {
            header(HttpHeaders.Authorization, "Bearer valid")
            // Intentionally NO Accept-Language header
        }

        // Implementation completed - endpoint now works without Accept-Language header
        // We expect OK status and response containing all translations
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
}
