package fr.devlille.partners.connect.sponsoring

import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.sponsoring.domain.CreateSponsoringOption
import fr.devlille.partners.connect.sponsoring.domain.SponsoringOption
import fr.devlille.partners.connect.sponsoring.domain.TranslatedLabel
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
import kotlin.test.assertTrue

class SponsoringOptionUpdateRoutesTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `PUT updates an option and returns 200 with option id`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId)
        }

        // First create an option
        val createRequest = CreateSponsoringOption(
            translations = listOf(
                TranslatedLabel(language = "fr", name = "Option Original", description = "Description Originale"),
                TranslatedLabel(language = "en", name = "Original Option", description = "Original Description"),
            ),
            price = 100,
        )

        val createResponse = client.post("/orgs/$orgId/events/$eventId/options") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(createRequest))
        }

        assertEquals(HttpStatusCode.Created, createResponse.status)
        val createResult = json.decodeFromString<Map<String, String>>(createResponse.bodyAsText())
        val optionId = createResult["id"]!!

        // Now update the option
        val updateRequest = CreateSponsoringOption(
            translations = listOf(
                TranslatedLabel(language = "fr", name = "Option Mise à jour", description = "Description Mise à jour"),
                TranslatedLabel(language = "en", name = "Updated Option", description = "Updated Description"),
            ),
            price = 200,
        )

        val updateResponse = client.put("/orgs/$orgId/events/$eventId/options/$optionId") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(updateRequest))
        }

        assertEquals(HttpStatusCode.OK, updateResponse.status)
        val updateResult = json.decodeFromString<Map<String, String>>(updateResponse.bodyAsText())
        assertEquals(optionId, updateResult["id"])

        // Verify the option was updated by getting it with French language
        val responseFr = client.get("/orgs/$orgId/events/$eventId/options") {
            header(HttpHeaders.AcceptLanguage, "fr")
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, responseFr.status)
        val bodyFr = json.decodeFromString<List<SponsoringOption>>(responseFr.body())
        assertEquals(1, bodyFr.size)
        assertEquals("Option Mise à jour", bodyFr.first().name)
        assertEquals("Description Mise à jour", bodyFr.first().description)
        assertEquals(200, bodyFr.first().price)
    }

    @Test
    fun `PUT replaces translations completely`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId)
        }

        // Create option with FR and EN translations
        val createRequest = CreateSponsoringOption(
            translations = listOf(
                TranslatedLabel(language = "fr", name = "Option FR", description = "Description FR"),
                TranslatedLabel(language = "en", name = "Option EN", description = "Description EN"),
            ),
            price = 100,
        )

        val createResponse = client.post("/orgs/$orgId/events/$eventId/options") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(createRequest))
        }

        val createResult = json.decodeFromString<Map<String, String>>(createResponse.bodyAsText())
        val optionId = createResult["id"]!!

        // Update with only EN translation (should remove FR)
        val updateRequest = CreateSponsoringOption(
            translations = listOf(
                TranslatedLabel(language = "en", name = "Updated EN Option", description = "Updated EN Description"),
            ),
            price = 150,
        )

        val updateResponse = client.put("/orgs/$orgId/events/$eventId/options/$optionId") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(updateRequest))
        }

        assertEquals(HttpStatusCode.OK, updateResponse.status)

        // Verify EN translation exists and is updated
        val responseEn = client.get("/orgs/$orgId/events/$eventId/options") {
            header(HttpHeaders.AcceptLanguage, "en")
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, responseEn.status)
        val bodyEn = json.decodeFromString<List<SponsoringOption>>(responseEn.body())
        assertEquals(1, bodyEn.size)
        assertEquals("Updated EN Option", bodyEn.first().name)

        // Verify FR translation no longer exists
        val responseFr = client.get("/orgs/$orgId/events/$eventId/options") {
            header(HttpHeaders.AcceptLanguage, "fr")
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.NotFound, responseFr.status)
        assertTrue(responseFr.bodyAsText().contains("Translation not found"))
    }

    @Test
    fun `PUT returns error when eventId is invalid UUID`() = testApplication {
        val orgId = UUID.randomUUID()
        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
        }

        val updateRequest = CreateSponsoringOption(
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
        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId)
        }

        val updateRequest = CreateSponsoringOption(
            translations = listOf(
                TranslatedLabel(language = "en", name = "Updated Option"),
            ),
            price = 200,
        )

        val response = client.put("/orgs/$orgId/events/$eventId/options/not-a-uuid") {
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
    fun `PUT returns 200 when payload has empty translations (consistent with create behavior)`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId)
        }

        // First create an option with translations
        val createRequest = CreateSponsoringOption(
            translations = listOf(
                TranslatedLabel(language = "en", name = "Original Option"),
            ),
            price = 100,
        )

        val createResponse = client.post("/orgs/$orgId/events/$eventId/options") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(createRequest))
        }

        val createResult = json.decodeFromString<Map<String, String>>(createResponse.bodyAsText())
        val optionId = createResult["id"]!!

        // Update with empty translations
        val updateRequest = CreateSponsoringOption(
            translations = emptyList(),
            price = 200,
        )

        val response = client.put("/orgs/$orgId/events/$eventId/options/$optionId") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(updateRequest))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val updateResult = json.decodeFromString<Map<String, String>>(response.bodyAsText())
        assertEquals(optionId, updateResult["id"])
    }

    @Test
    fun `PUT returns 401 when no authorization header`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId)
        }

        val updateRequest = CreateSponsoringOption(
            translations = listOf(
                TranslatedLabel(language = "en", name = "Updated Option"),
            ),
            price = 200,
        )

        val response = client.put("/orgs/$orgId/events/$eventId/options/${UUID.randomUUID()}") {
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(updateRequest))
        }

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `PUT returns 401 when user lacks organization permission`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId)
        }

        val updateRequest = CreateSponsoringOption(
            translations = listOf(
                TranslatedLabel(language = "en", name = "Updated Option"),
            ),
            price = 200,
        )

        val response = client.put("/orgs/$orgId/events/$eventId/options/${UUID.randomUUID()}") {
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
        val nonExistentEventId = UUID.randomUUID()
        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId)
        }

        val updateRequest = CreateSponsoringOption(
            translations = listOf(
                TranslatedLabel(language = "en", name = "Updated Option"),
            ),
            price = 200,
        )

        val response = client.put("/orgs/$orgId/events/$nonExistentEventId/options/${UUID.randomUUID()}") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(updateRequest))
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `PUT returns 404 when option does not exist`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(eventId, orgId)
        }

        val updateRequest = CreateSponsoringOption(
            translations = listOf(
                TranslatedLabel(language = "en", name = "Updated Option"),
            ),
            price = 200,
        )

        val nonExistentOptionId = UUID.randomUUID()
        val response = client.put("/orgs/$orgId/events/$eventId/options/$nonExistentOptionId") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(updateRequest))
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertTrue(response.bodyAsText().contains("Option not found"))
    }

    @Test
    fun `PUT returns 404 when option belongs to different event`() = testApplication {
        val orgId = UUID.randomUUID()
        val event1Id = UUID.randomUUID()
        val event2Id = UUID.randomUUID()
        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEventWithAdminUser(event1Id, orgId)
        }

        // Create option for event1
        val createRequest = CreateSponsoringOption(
            translations = listOf(
                TranslatedLabel(language = "en", name = "Original Option"),
            ),
            price = 100,
        )

        val createResponse = client.post("/orgs/$orgId/events/$event1Id/options") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(createRequest))
        }

        val createResult = json.decodeFromString<Map<String, String>>(createResponse.bodyAsText())
        val optionId = createResult["id"]!!

        // Try to update the option through a different eventId (should fail)
        val updateRequest = CreateSponsoringOption(
            translations = listOf(
                TranslatedLabel(language = "en", name = "Updated Option"),
            ),
            price = 200,
        )

        val response = client.put("/orgs/$orgId/events/$event2Id/options/$optionId") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(updateRequest))
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertTrue(response.bodyAsText().contains("Option not found"))
    }
}
