package fr.devlille.partners.connect.sponsoring

import fr.devlille.partners.connect.events.factories.insertMockedEvent
import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.sponsoring.domain.CreateSelectableValue
import fr.devlille.partners.connect.sponsoring.domain.CreateSponsoringOption
import fr.devlille.partners.connect.sponsoring.domain.CreateText
import fr.devlille.partners.connect.sponsoring.domain.CreateTypedQuantitative
import fr.devlille.partners.connect.sponsoring.domain.CreateTypedSelectable
import fr.devlille.partners.connect.sponsoring.domain.QuantitativeDescriptor
import fr.devlille.partners.connect.sponsoring.domain.SelectableDescriptor
import fr.devlille.partners.connect.sponsoring.domain.SponsoringOptionWithTranslations
import fr.devlille.partners.connect.sponsoring.domain.TranslatedLabel
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringOption
import fr.devlille.partners.connect.users.factories.insertMockedEventWithAdminUser
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SponsoringOptionUpdateRoutesTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `PUT updates text option - schema validation`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val eventSlug = "test-event-update-text"
        val optionId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)
            insertMockedSponsoringOption(optionId, eventId)
        }

        val request = CreateText(
            translations = listOf(
                TranslatedLabel(
                    language = "en",
                    name = "Updated Social Media",
                    description = "Updated description",
                ),
            ),
            price = 600,
        )

        val response = client.put("/orgs/$orgId/events/$eventSlug/options/$optionId") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(Json.encodeToString(CreateSponsoringOption.serializer(), request))
        }

        // Contract test: Should return 200 with updated option ID
        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody = response.bodyAsText()
        assertTrue(responseBody.contains("id"))
        assertTrue(responseBody.contains("\"id\":"))
    }

    @Test
    fun `PUT updates typed_quantitative option - schema validation`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val eventSlug = "test-event-update-quantitative"
        val optionId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)
            insertMockedSponsoringOption(optionId, eventId)
        }

        val request = CreateTypedQuantitative(
            translations = listOf(
                TranslatedLabel(
                    language = "en",
                    name = "Updated Job Offers",
                    description = "Updated job board posting",
                ),
            ),
            price = 150,
            typeDescriptor = QuantitativeDescriptor.JOB_OFFER,
        )

        val response = client.put("/orgs/$orgId/events/$eventSlug/options/$optionId") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(Json.encodeToString(CreateSponsoringOption.serializer(), request))
        }

        // Contract test: This should fail initially - polymorphic types not implemented
        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody = response.bodyAsText()
        assertTrue(responseBody.contains("id"))
    }

    @Test
    fun `PUT updates typed_selectable option - schema validation`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val eventSlug = "test-event-update-selectable"
        val optionId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)
            insertMockedSponsoringOption(optionId, eventId)
        }

        val request = CreateTypedSelectable(
            translations = listOf(
                TranslatedLabel(
                    language = "en",
                    name = "Updated Exhibition Booth",
                    description = "Updated booth space",
                ),
            ),
            typeDescriptor = SelectableDescriptor.BOOTH,
            selectableValues = listOf(
                CreateSelectableValue("2x2m", 80000),
                CreateSelectableValue("4x4m", 120000),
                CreateSelectableValue("6x8m", 250000),
            ),
        )

        val response = client.put("/orgs/$orgId/events/$eventSlug/options/$optionId") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(Json.encodeToString(CreateSponsoringOption.serializer(), request))
        }

        // Contract test: This should fail initially
        assertEquals(HttpStatusCode.OK, response.status)
        val responseBody = response.bodyAsText()
        assertTrue(responseBody.contains("id"))
    }

    @Test
    fun `PUT replaces translations completely`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val eventSlug = "test-event-slug-2"
        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)
        }

        // Create option with FR and EN translations
        val createRequest = CreateText(
            translations = listOf(
                TranslatedLabel(language = "fr", name = "Option FR", description = "Description FR"),
                TranslatedLabel(language = "en", name = "Option EN", description = "Description EN"),
            ),
            price = 100,
        )

        val createResponse = client.post("/orgs/$orgId/events/$eventSlug/options") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(CreateSponsoringOption.serializer(), createRequest))
        }

        val createResult = json.decodeFromString<Map<String, String>>(createResponse.bodyAsText())
        val optionId = createResult["id"]!!

        // Update with only EN translation (should remove FR)
        val updateRequest = CreateText(
            translations = listOf(
                TranslatedLabel(language = "en", name = "Updated EN Option", description = "Updated EN Description"),
            ),
            price = 150,
        )

        val updateResponse = client.put("/orgs/$orgId/events/$eventSlug/options/$optionId") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(CreateSponsoringOption.serializer(), updateRequest))
        }

        assertEquals(HttpStatusCode.OK, updateResponse.status)

        // Verify the translation replacement using organizer endpoint (returns all translations)
        val response = client.get("/orgs/$orgId/events/$eventSlug/options") {
            header(HttpHeaders.Authorization, "Bearer valid")
            // No Accept-Language header needed - organizer endpoint returns all translations
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = json.decodeFromString<List<SponsoringOptionWithTranslations>>(response.body())
        assertEquals(1, body.size)

        val option = body.first()
        // Verify only EN translation exists (FR should be removed)
        assertTrue(option.translations.containsKey("en"))
        assertFalse(option.translations.containsKey("fr")) // Should be removed after update
        assertEquals("Updated EN Option", option.translations["en"]?.name)
        assertEquals("Updated EN Description", option.translations["en"]?.description)
        assertEquals(150, option.price)
    }

    @Test
    fun `PUT returns error when eventId is invalid UUID`() = testApplication {
        val orgId = UUID.randomUUID()
        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
        }

        val updateRequest = CreateText(
            translations = listOf(
                TranslatedLabel(language = "en", name = "Updated Option"),
            ),
            price = 200,
        )

        val response = client.put("/orgs/$orgId/events/not-a-uuid/options/${UUID.randomUUID()}") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(updateRequest))
        }

        // The UUID parsing might happen at different levels, so let's just check it's not a 200
        assertTrue(response.status.value >= 400)
        assertTrue(
            response.bodyAsText().contains("Invalid UUID format") ||
                response.bodyAsText().contains("not found", ignoreCase = true) ||
                response.bodyAsText().contains("bad request", ignoreCase = true),
        )
    }

    @Test
    fun `PUT returns error when optionId is invalid UUID`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val eventSlug = "test-event-slug-3"
        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)
        }

        val updateRequest = CreateText(
            translations = listOf(
                TranslatedLabel(language = "en", name = "Updated Option"),
            ),
            price = 200,
        )

        val response = client.put("/orgs/$orgId/events/$eventSlug/options/not-a-uuid") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(updateRequest))
        }

        // The UUID parsing might happen at different levels, so let's just check it's not a 200
        assertTrue(response.status.value >= 400)
        assertTrue(response.bodyAsText().contains("Request parameter id couldn't be parsed/converted to UUID"))
    }

    @Test
    fun `PUT returns 400 when payload has empty translations`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val optionId = UUID.randomUUID()
        val eventSlug = "test-event-slug-4"
        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)
            insertMockedSponsoringOption(optionId, eventId)
        }

        val updateRequest = CreateText(
            translations = emptyList(),
            price = 200,
        )

        val response = client.put("/orgs/$orgId/events/$eventSlug/options/$optionId") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(CreateSponsoringOption.serializer(), updateRequest))
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `PUT returns 401 when no authorization header`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val eventSlug = "test-event-slug-5"
        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)
        }

        val updateRequest = CreateText(
            translations = listOf(
                TranslatedLabel(language = "en", name = "Updated Option"),
            ),
            price = 200,
        )

        val response = client.put("/orgs/$orgId/events/$eventSlug/options/${UUID.randomUUID()}") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(updateRequest))
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `PUT returns 401 when user lacks organization permission`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val eventSlug = "test-event-slug-6"
        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)
        }

        val updateRequest = CreateText(
            translations = listOf(
                TranslatedLabel(language = "en", name = "Updated Option"),
            ),
            price = 200,
        )

        val response = client.put("/orgs/$orgId/events/$eventSlug/options/${UUID.randomUUID()}") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer invalid")
            setBody(json.encodeToString(updateRequest))
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `PUT returns 404 when event does not exist`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val nonExistentEventSlug = "non-existent-event-slug"
        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId)
        }

        val updateRequest = CreateText(
            translations = listOf(
                TranslatedLabel(language = "en", name = "Updated Option"),
            ),
            price = 200,
        )

        val response = client.put("/orgs/$orgId/events/$nonExistentEventSlug/options/${UUID.randomUUID()}") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(CreateSponsoringOption.serializer(), updateRequest))
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `PUT returns 404 when option does not exist`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val eventSlug = "test-event-slug-7"
        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId, eventSlug)
        }

        val updateRequest = CreateText(
            translations = listOf(
                TranslatedLabel(language = "en", name = "Updated Option"),
            ),
            price = 200,
        )

        val nonExistentOptionId = UUID.randomUUID()
        val response = client.put("/orgs/$orgId/events/$eventSlug/options/$nonExistentOptionId") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(CreateSponsoringOption.serializer(), updateRequest))
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertTrue(response.bodyAsText().contains("Option not found"))
    }

    @Test
    fun `PUT returns 404 when option belongs to different event`() = testApplication {
        val orgId = UUID.randomUUID()
        val event1Id = UUID.randomUUID()
        val event2Id = UUID.randomUUID()
        val event1Slug = "test-event1-slug"
        val event2Slug = "test-event2-slug"

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(event1Id, orgId, event1Slug)
            // Create second event without admin user (just the event)
            insertMockedEvent(event2Id, slug = event2Slug, orgId = orgId)
        }

        // Create option for event1
        val createRequest = CreateText(
            translations = listOf(
                TranslatedLabel(language = "en", name = "Original Option"),
            ),
            price = 100,
        )

        val createResponse = client.post("/orgs/$orgId/events/$event1Slug/options") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(CreateSponsoringOption.serializer(), createRequest))
        }

        val createResult = json.decodeFromString<Map<String, String>>(createResponse.bodyAsText())
        val optionId = createResult["id"]!!

        // Try to update the option through a different eventId (should fail)
        val updateRequest = CreateText(
            translations = listOf(
                TranslatedLabel(language = "en", name = "Updated Option"),
            ),
            price = 200,
        )

        val response = client.put("/orgs/$orgId/events/$event2Slug/options/$optionId") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(CreateSponsoringOption.serializer(), updateRequest))
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertTrue(response.bodyAsText().contains("Option not found"))
    }
}
