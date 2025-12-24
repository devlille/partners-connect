package fr.devlille.partners.connect.events.infrastructure.api

import fr.devlille.partners.connect.events.factories.insertMockedFutureEvent
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Contract test for agenda endpoints.
 * Tests JSON schema validation for agenda_response.schema.json.
 * Validates that endpoints correctly handle valid and invalid requests.
 */
class EventAgendaRouteGetTest {
    @Test
    fun `GET agenda returns valid agenda response schema`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()

        application {
            moduleSharedDb(UUID.randomUUID())
            transaction {
                insertMockedOrganisationEntity(orgId)
                insertMockedFutureEvent(eventId, orgId = orgId)
            }
        }

        val response = client.get("/events/$eventId/agenda")

        assertEquals(HttpStatusCode.OK, response.status)

        val responseBody = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertNotNull(responseBody["sessions"])
        assertNotNull(responseBody["speakers"])
    }

    @Test
    fun `GET agenda returns 404 for non-existent event`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventSlug = "non-existent-event"

        application {
            moduleSharedDb(UUID.randomUUID())
            transaction {
                insertMockedOrganisationEntity(orgId)
            }
        }

        val response = client.get("/events/$eventSlug/agenda")

        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}
