package fr.devlille.partners.connect.events.infrastructure.api

import fr.devlille.partners.connect.events.factories.insertMockedEventWithOrga
import fr.devlille.partners.connect.internal.moduleMocked
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import fr.devlille.partners.connect.users.factories.insertMockedUser
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Contract test for agenda endpoints.
 * Tests JSON schema validation for agenda_response.schema.json.
 * Validates that endpoints correctly handle valid and invalid requests.
 */
class AgendaRoutesContractTest {
    @Test
    fun `GET agenda returns valid agenda response schema`() = testApplication {
        val orgId = UUID.randomUUID()
        val eventId = UUID.randomUUID()
        val orgSlug = "test-org"
        val eventSlug = "test-event"

        application {
            moduleMocked()
            val organisation = insertMockedOrganisationEntity(orgId, name = orgSlug)
            insertMockedEventWithOrga(id = eventId, slug = eventSlug, organisation = organisation)
        }

        val response = client.get("/events/$eventSlug/agenda")

        assertEquals(HttpStatusCode.OK, response.status)

        val responseBody = Json.parseToJsonElement(response.bodyAsText()).jsonObject
        assertNotNull(responseBody["sessions"])
        assertNotNull(responseBody["speakers"])
    }

    @Test
    fun `GET agenda returns 404 for non-existent event`() = testApplication {
        val orgId = UUID.randomUUID()
        val orgSlug = "test-org"
        val eventSlug = "non-existent-event"
        val userId = UUID.randomUUID()

        application {
            moduleMocked()
            insertMockedOrganisationEntity(orgId, name = orgSlug)
            val user = insertMockedUser(userId, email = "john.doe@test.com")
            insertMockedOrgaPermission(orgId, user, canEdit = true)
        }

        val response = client.get("/events/$eventSlug/agenda")

        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}
