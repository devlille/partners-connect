package fr.devlille.partners.connect.events.infrastructure.api

import fr.devlille.partners.connect.events.domain.Event
import fr.devlille.partners.connect.events.factories.createEvent
import fr.devlille.partners.connect.internal.moduleSharedDb
import fr.devlille.partners.connect.organisations.factories.insertMockedOrganisationEntity
import fr.devlille.partners.connect.users.factories.insertMockedOrgaPermission
import fr.devlille.partners.connect.users.factories.insertMockedUser
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
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class EventCreationRoutePostTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `POST creates an event and grants access to creator`() = testApplication {
        val userId = UUID.randomUUID()
        val orgId = UUID.randomUUID()

        application {
            moduleSharedDb(userId = userId)
            transaction {
                insertMockedOrganisationEntity(id = orgId)
                insertMockedUser(userId)
                insertMockedOrgaPermission(orgId = orgId, userId = userId)
            }
        }

        val response = client.post("/orgs/$orgId/events") {
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Authorization, "Bearer valid")
            setBody(json.encodeToString(Event.serializer(), createEvent()))
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val responseText = response.bodyAsText()
        val responseBody = Json.decodeFromString<Map<String, String>>(responseText)
        assertNotNull(responseBody["slug"], "Response should contain a 'slug' field")
    }
}
