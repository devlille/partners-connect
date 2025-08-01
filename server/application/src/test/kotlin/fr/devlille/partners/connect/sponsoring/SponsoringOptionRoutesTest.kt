package fr.devlille.partners.connect.sponsoring

import fr.devlille.partners.connect.internal.insertMockedAdminUser
import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.sponsoring.domain.CreateSponsoringOption
import fr.devlille.partners.connect.sponsoring.domain.SponsoringOption
import fr.devlille.partners.connect.sponsoring.domain.TranslatedLabel
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
        val eventId = UUID.randomUUID()
        application {
            moduleMocked()
            insertMockedAdminUser(eventId)
        }

        val response = client.get("/events/$eventId/options") {
            header(HttpHeaders.AcceptLanguage, "fr")
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("[]", response.bodyAsText())
    }

    @Test
    fun `POST creates an option with translations and GET returns it`() = testApplication {
        val eventId = UUID.randomUUID()
        application {
            moduleMocked()
            insertMockedAdminUser(eventId)
        }

        val request = CreateSponsoringOption(
            translations = listOf(
                TranslatedLabel(language = "fr", name = "Option FR", description = "Description FR"),
                TranslatedLabel(language = "en", name = "Option EN", description = "Description EN"),
            ),
            price = 100,
        )

        val postResponse = client.post("/events/$eventId/options") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(request))
        }

        assertEquals(HttpStatusCode.Created, postResponse.status)

        val response = json.decodeFromString<Map<String, String>>(postResponse.bodyAsText())
        assertNotNull(response["id"])

        val responseFr = client.get("/events/$eventId/options") {
            header(HttpHeaders.AcceptLanguage, "fr")
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, responseFr.status)
        val bodyFr = json.decodeFromString<List<SponsoringOption>>(responseFr.body())
        assertEquals(1, bodyFr.size)
        assertEquals("Option FR", bodyFr.first().name)

        val responseEn = client.get("/events/$eventId/options") {
            header(HttpHeaders.AcceptLanguage, "en")
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.OK, responseEn.status)
        val bodyEn = json.decodeFromString<List<SponsoringOption>>(responseEn.body())
        assertEquals(1, bodyEn.size)
        assertEquals("Option EN", bodyEn.first().name)
    }

    @Test
    fun `GET returns 400 when Accept-Language is missing`() = testApplication {
        val eventId = UUID.randomUUID()
        application {
            moduleMocked()
            insertMockedAdminUser(eventId)
        }

        val response = client.get("/events/$eventId/options") {
            header(HttpHeaders.Authorization, "Bearer valid")
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("accept-language", ignoreCase = true))
    }

    @Test
    fun `POST and GET fail if translation for requested language doesn't exist`() = testApplication {
        val eventId = UUID.randomUUID()
        application {
            moduleMocked()
            insertMockedAdminUser(eventId)
        }

        val request = CreateSponsoringOption(
            translations = listOf(
                TranslatedLabel(language = "fr", name = "Nom FR"),
            ),
        )

        client.post("/events/$eventId/options") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(request))
        }

        val response = client.get("/events/$eventId/options") {
            header(HttpHeaders.AcceptLanguage, "de")
            header(HttpHeaders.Authorization, "Bearer valid")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
        assertTrue(response.bodyAsText().contains("Translation not found"))
    }
}
