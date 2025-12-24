package fr.devlille.partners.connect.sponsoring

import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.sponsoring.domain.CreateSponsoringOption
import fr.devlille.partners.connect.sponsoring.domain.CreateText
import fr.devlille.partners.connect.sponsoring.domain.SponsoringOptionWithTranslations
import fr.devlille.partners.connect.sponsoring.domain.TranslatedLabel
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import fr.devlille.partners.connect.users.factories.insertMockedUser
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
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SponsoringOptionUpdateRoutesTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `PUT replaces translations completely`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(eventId, orgId = orgId)
            }
        }

        // Create option with FR and EN translations
        val createRequest = CreateText(
            translations = listOf(
                TranslatedLabel(language = "fr", name = "Option FR", description = "Description FR"),
                TranslatedLabel(language = "en", name = "Option EN", description = "Description EN"),
            ),
            price = 100,
        )

        val createResponse = client.post("/orgs/$orgId/events/$eventId/options") {
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

        val updateResponse = client.put("/orgs/$orgId/events/$eventId/options/$optionId") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(CreateSponsoringOption.serializer(), updateRequest))
        }

        assertEquals(HttpStatusCode.OK, updateResponse.status)

        // Verify the translation replacement using organizer endpoint (returns all translations)
        val response = client.get("/orgs/$orgId/events/$eventId/options") {
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
    fun `PUT returns 404 when option belongs to different event`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val event1Id = UUID.randomUUID()
        val event2Id = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedUser(userId)
                insertMockedOrganisationEntity(orgId)
                insertMockedOrgaPermission(orgId, userId = userId)
                insertMockedFutureEvent(event1Id, orgId = orgId)
                insertMockedFutureEvent(event2Id, orgId = orgId)
            }
        }

        // Create option for event1
        val createRequest = CreateText(
            translations = listOf(
                TranslatedLabel(language = "en", name = "Original Option"),
            ),
            price = 100,
        )

        val createResponse = client.post("/orgs/$orgId/events/$event1Id/options") {
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

        val response = client.put("/orgs/$orgId/events/$event2Id/options/$optionId") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(CreateSponsoringOption.serializer(), updateRequest))
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertTrue(response.bodyAsText().contains("Option not found"))
    }
}
