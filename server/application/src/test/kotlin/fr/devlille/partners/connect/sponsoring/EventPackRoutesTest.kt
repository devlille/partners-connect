package fr.devlille.partners.connect.sponsoring

import fr.devlille.partners.connect.events.factories.insertMockedEvent
import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringPack
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EventPackRoutesTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `GET returns empty list when no packs exist`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val eventSlug = "test-event-slug-1"
        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEvent(id = eventId, orgId = orgId, slug = eventSlug)
        }

        val client = createClient { install(ContentNegotiation) { json() } }
        val response = client.get("/events/$eventSlug/sponsoring/packs") {
            header(HttpHeaders.AcceptLanguage, "fr")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("[]", response.bodyAsText())
    }

    @Test
    fun `GET returns packs for event`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val eventSlug = "test-event-slug-2"
        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEvent(id = eventId, orgId = orgId, slug = eventSlug)
            insertMockedSponsoringPack(event = eventId)
        }

        val client = createClient { install(ContentNegotiation) { json() } }
        val response = client.get("/events/$eventSlug/sponsoring/packs") {
            header(HttpHeaders.AcceptLanguage, "fr")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val arr = json.parseToJsonElement(response.bodyAsText()).jsonArray
        assertTrue(arr.size > 0)
    }

    @Test
    fun `GET returns 404 for unknown event`() = testApplication {
        application {
            moduleMocked()
        }

        val client = createClient { install(ContentNegotiation) { json() } }
        val response = client.get("/events/unknown-event/sponsoring/packs") {
            header(HttpHeaders.AcceptLanguage, "fr")
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `GET returns 400 when Accept-Language header is missing`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val eventSlug = "test-event-slug-3"
        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId)
            insertMockedEvent(id = eventId, orgId = orgId, slug = eventSlug)
        }

        val client = createClient { install(ContentNegotiation) { json() } }
        val response = client.get("/events/$eventSlug/sponsoring/packs") {
            // Intentionally omit Accept-Language header
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }
}
