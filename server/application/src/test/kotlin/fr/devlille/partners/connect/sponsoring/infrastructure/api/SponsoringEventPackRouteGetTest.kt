package fr.devlille.partners.connect.sponsoring.infrastructure.api

import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.sponsoring.factories.insertMockedSponsoringPack
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SponsoringEventPackRouteGetTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `GET returns empty list when no packs exist`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedOrganisationEntity(orgId)
                insertMockedFutureEvent(id = eventId, orgId = orgId)
            }
        }

        val response = client.get("/events/$eventId/sponsoring/packs") {
            header(HttpHeaders.AcceptLanguage, "fr")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("[]", response.bodyAsText())
    }

    @Test
    fun `GET returns packs for event`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedOrganisationEntity(orgId)
                insertMockedFutureEvent(id = eventId, orgId = orgId)
                insertMockedSponsoringPack(eventId = eventId)
            }
        }

        val response = client.get("/events/$eventId/sponsoring/packs") {
            header(HttpHeaders.AcceptLanguage, "fr")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val arr = json.parseToJsonElement(response.bodyAsText()).jsonArray
        assertTrue(arr.isNotEmpty())
    }

    @Test
    fun `GET returns 404 for unknown event`() = testApplication {
        val userId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
        }

        val response = client.get("/events/unknown-event/sponsoring/packs") {
            header(HttpHeaders.AcceptLanguage, "fr")
        }
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `GET returns 400 when Accept-Language header is missing`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()

        application {
            moduleSharedDb(userId)
            transaction {
                insertMockedOrganisationEntity(orgId)
                insertMockedFutureEvent(id = eventId, orgId = orgId)
            }
        }

        val response = client.get("/events/$eventId/sponsoring/packs") {
            // Intentionally omit Accept-Language header
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }
}
